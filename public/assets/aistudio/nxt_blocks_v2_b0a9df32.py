#!/usr/bin/env python3
"""NXT Blocks v2: Scratch-style block programming for a LEGO NXT brick, built on NXT-Python.

Run:  source ~/nxt-env/bin/activate && python3 nxt_blocks_v2.py
Fixed wiring: touch sensors on ports 1 and 2, color sensor on port 3, ultrasonic on port 4,
motors on ports A, B and C. The page is served only on this Mac (127.0.0.1).
"""
import json
import os
import secrets
import threading
import time
import webbrowser
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

os.environ.setdefault("DYLD_FALLBACK_LIBRARY_PATH", "/opt/homebrew/lib")

import nxt.locator
import nxt.motor
import nxt.sensor
import nxt.sensor.generic as gen

PORT = 8766
TOKEN = secrets.token_urlsafe(16)
STATE = {"running": False, "log": [], "stop": threading.Event()}
SENSORS = {"t1": (1, gen.Touch, "Touch 1"), "t2": (2, gen.Touch, "Touch 2"),
           "color": (3, gen.Color, "Color"), "dist": (4, gen.Ultrasonic, "Distance")}


class Stopped(Exception):
    pass


class Runner:
    """Interprets the block program. Only known block types are ever executed."""

    def __init__(self, brick, stop, log):
        self.b, self.stop, self.log, self.cache = brick, stop, log, {}

    def nap(self, sec):
        end = time.time() + sec
        while True:
            if self.stop.is_set():
                raise Stopped
            left = end - time.time()
            if left <= 0:
                return
            time.sleep(min(0.05, left))

    def motors(self, name):
        out = []
        for c in str(name).split("+"):
            if c not in ("A", "B", "C"):
                raise ValueError("bad motor: " + c)
            if "m" + c not in self.cache:
                self.cache["m" + c] = self.b.get_motor(getattr(nxt.motor.Port, c))
            out.append(self.cache["m" + c])
        return out

    def sensor(self, k):
        if k not in self.cache:
            port, cls, _ = SENSORS[k]
            self.cache[k] = self.b.get_sensor(getattr(nxt.sensor.Port, "S%d" % port), cls)
        return self.cache[k]

    def test(self, c):
        k, op, v = c["k"], c["op"], c.get("v")
        val = self.sensor(k).get_sample()
        if k in ("t1", "t2"):
            return bool(val) == (op == "pressed")
        if k == "dist":
            return val < float(v) if op == "<" else val > float(v)
        return (val.name.lower() == str(v).lower()) == (op == "is")

    def run(self, blocks):
        for blk in blocks:
            if self.stop.is_set():
                raise Stopped
            t, a, c = blk.get("t"), blk.get("a", {}), blk.get("c", [])
            if t in ("motor_on", "motor_for"):
                power = max(-100, min(100, int(float(a["p"]))))
                ms = self.motors(a["m"])
                for m in ms:
                    m.run(power)
                if t == "motor_for":
                    self.nap(max(0.0, float(a["s"])))
                    for m in ms:
                        m.brake()
            elif t == "motor_off":
                for m in self.motors(a["m"]):
                    m.brake()
            elif t == "wait":
                self.nap(max(0.0, float(a["s"])))
            elif t == "wait_until":
                while not self.test(blk["cond"]):
                    self.nap(0.03)
            elif t == "show":
                v = self.sensor(a["k"]).get_sample()
                self.log.append("%s: %s" % (SENSORS[a["k"]][2], getattr(v, "name", v)))
            elif t == "beep":
                ms = max(10, int(float(a["ms"])))
                self.b.play_tone(int(float(a["hz"])), ms)
                self.nap(ms / 1000.0)
            elif t == "repeat":
                for _ in range(max(0, int(float(a["n"])))):
                    self.run(c)
                    self.nap(0.02)
            elif t == "forever":
                while True:
                    self.run(c)
                    self.nap(0.03)
            elif t == "if":
                if self.test(blk["cond"]):
                    self.run(c)
            elif t == "until":
                while not self.test(blk["cond"]):
                    self.run(c)
                    self.nap(0.03)
            else:
                raise ValueError("unknown block: %r" % (t,))


def worker(prog, stop, log):
    try:
        log.append("Connecting to brick...")
        with nxt.locator.find() as brick:
            log.append("Connected: %s" % brick.get_device_info()[0])
            r = Runner(brick, stop, log)
            try:
                r.run(prog)
            finally:
                for key, m in list(r.cache.items()):
                    if key[0] == "m":
                        try:
                            m.idle()
                        except Exception:
                            pass
        log.append("Finished.")
    except Stopped:
        log.append("Stopped.")
    except Exception as e:
        log.append("Error: %s" % e)
    finally:
        STATE["running"] = False


class Handler(BaseHTTPRequestHandler):
    def log_message(self, *args):
        pass

    def send(self, code, body, ctype="application/json"):
        data = body.encode("utf-8")
        self.send_response(code)
        self.send_header("Content-Type", ctype)
        self.send_header("Content-Length", str(len(data)))
        self.end_headers()
        self.wfile.write(data)

    def host_ok(self):
        return self.headers.get("Host") in ("127.0.0.1:%d" % PORT, "localhost:%d" % PORT)

    def do_GET(self):
        if not self.host_ok():
            return self.send(403, "{}")
        if self.path == "/":
            self.send(200, PAGE.replace("__TOKEN__", TOKEN), "text/html; charset=utf-8")
        elif self.path == "/status":
            self.send(200, json.dumps({"running": STATE["running"], "log": STATE["log"][-200:]}))
        else:
            self.send(404, "{}")

    def do_POST(self):
        if not self.host_ok() or self.headers.get("X-Token") != TOKEN:
            return self.send(403, "{}")
        raw = self.rfile.read(int(self.headers.get("Content-Length", 0)))
        if self.path == "/stop":
            STATE["stop"].set()
            return self.send(200, "{}")
        if self.path == "/run":
            if STATE["running"]:
                return self.send(200, json.dumps({"error": "Already running. Press Stop first."}))
            try:
                prog = json.loads(raw)
                assert isinstance(prog, list)
            except Exception:
                return self.send(200, json.dumps({"error": "Bad program."}))
            STATE["stop"], STATE["log"], STATE["running"] = threading.Event(), [], True
            threading.Thread(target=worker, args=(prog, STATE["stop"], STATE["log"]), daemon=True).start()
            return self.send(200, "{}")
        self.send(404, "{}")


PAGE = r"""<!doctype html>
<html><head><meta charset="utf-8"><title>NXT Blocks</title>
<style>
*{box-sizing:border-box}
body{margin:0;font:14px system-ui,sans-serif;background:#f9f9f9;color:#222;height:100vh;display:flex;flex-direction:column}
header{background:#4c97ff;color:#fff;padding:8px 14px;display:flex;gap:8px;align-items:center}
header h1{font-size:17px;margin:0 auto 0 0}
button{font:inherit;border:0;border-radius:16px;padding:5px 14px;cursor:pointer}
#run{background:#4cbf56;color:#fff}#stop{background:#ec5959;color:#fff}
main{flex:1;display:flex;min-height:0}
#lt{display:flex;width:310px;background:#fff;border-right:1px solid #ddd}
#cats{width:96px;display:flex;flex-direction:column;gap:4px;padding:8px 0}
.cat{background:none;border-radius:0;text-align:left;padding:10px 6px}.cat.on{background:#e8f0ff;font-weight:600}
#blocks{flex:1;padding:10px 8px;overflow:auto}
#blocks .blk{margin-bottom:10px;cursor:grab}
#prog{flex:1;padding:16px;overflow:auto;background:#f0f2f5}
#rt{width:250px;padding:10px;background:#fff;border-left:1px solid #ddd;overflow:auto}
.blk{--c:#888;position:relative}
.top,.bot{background:var(--c);color:#fff;padding:7px 9px;display:flex;flex-wrap:wrap;align-items:center;gap:5px;font-weight:600}
.blk:not(.cb)>.top{border-radius:5px}
.cb>.top{border-radius:5px 5px 0 0}
.bot{height:16px;padding:0;border-radius:0 0 5px 5px}
.kids{border-left:14px solid var(--c);min-height:8px}
.top::before{content:"";position:absolute;left:14px;top:-4px;width:22px;height:5px;background:var(--c);border-radius:4px 4px 0 0}
.hat{background:#ffbf00;color:#fff;font-weight:600;padding:12px 14px 7px;border-radius:20px 20px 5px 5px;width:fit-content}
.slot{height:4px}.slot.on{height:26px;background:rgba(0,0,0,.12);border-radius:4px}
.kids>.slot:only-child{height:22px}
input,select{border:0;border-radius:11px;padding:2px 8px;background:#fff;color:#333;font:inherit;font-weight:400}
input{width:58px}
.pill{background:#fff;color:#333;border-radius:11px;padding:1px 8px;font-weight:400}
.cond{display:inline-flex;gap:3px;align-items:center;background:rgba(0,0,0,.22);padding:2px 14px;clip-path:polygon(10px 0,calc(100% - 10px) 0,100% 50%,calc(100% - 10px) 100%,10px 100%,0 50%)}
.del{margin-left:auto;background:rgba(0,0,0,.2);color:#fff;padding:0 7px;border-radius:9px}
pre{background:#222;color:#9f9;padding:8px;border-radius:6px;min-height:160px;white-space:pre-wrap}
.hint{color:#666;font-size:12px}
</style></head><body>
<header><h1>NXT Blocks</h1><span id="st">idle</span>
<button id="ex">Example</button><button id="clr">Clear</button>
<button id="run">&#9654; Run</button><button id="stop">&#9632; Stop</button></header>
<main><div id="lt"><div id="cats"></div><div id="blocks"></div></div>
<div id="prog"></div>
<div id="rt"><b>Log</b><pre id="log"></pre>
<p class="hint">Touch 1 and 2: ports 1, 2. Color: port 3. Distance (ultrasonic): port 4. Motors: A, B, C.<br>Drag blocks from the left into the script, drag them back to the left to delete, or click a block to add it at the end.</p></div></main>
<script>
const TOKEN="__TOKEN__",$=id=>document.getElementById(id);
const MT=['A','B','C','A+B','A+C','B+C','A+B+C'];
const SN=[['t1','Touch 1'],['t2','Touch 2'],['dist','Distance'],['color','Color']];
const COL=['black','blue','green','yellow','red','white'];
const OPS={t1:['pressed','not pressed'],t2:['pressed','not pressed'],dist:['<','>'],color:['is','is not']};
const M={k:'m',o:MT,d:'A'},CD={cond:1};
const D={
 motor_on:['Motor',M,'on at power',{k:'p',d:60}],
 motor_for:['Run motor',M,'at power',{k:'p',d:60},'for',{k:'s',d:1},'sec'],
 motor_off:['Stop motor',M],
 wait_until:['Wait until',CD],
 show:['Show',{k:'k',o:SN,d:'dist'},'in log'],
 wait:['Wait',{k:'s',d:1},'seconds'],
 repeat:['Repeat',{k:'n',d:10},'times'],
 forever:['Forever'],
 if:['If',CD,'then'],
 until:['Repeat until',CD],
 beep:['Beep',{k:'hz',d:440},'Hz for',{k:'ms',d:300},'ms']};
const BOX={repeat:1,forever:1,if:1,until:1};
const CATS=[{n:'Motors',c:'#4c97ff',b:['motor_on','motor_for','motor_off']},
 {n:'Sensing',c:'#2fb4d8',b:['wait_until','show']},
 {n:'Control',c:'#ffab19',b:['wait','repeat','forever','if','until']},
 {n:'Sound',c:'#cf63cf',b:['beep']}];
const CC={};CATS.forEach(c=>c.b.forEach(t=>CC[t]=c.c));
let prog=[],cur=0,drag=null;
try{prog=JSON.parse(localStorage.getItem('nxtprog2'))||[];if(!Array.isArray(prog))prog=[]}catch(e){prog=[]}
function save(){try{localStorage.setItem('nxtprog2',JSON.stringify(prog))}catch(e){}}
function mk(tag,txt,cls){const e=document.createElement(tag);if(txt)e.textContent=txt;if(cls)e.className=cls;return e}
function sel(o,v,fn){const s=mk('select');o.forEach(x=>{const p=Array.isArray(x)?x:[x,x],op=mk('option',p[1]);op.value=p[0];s.append(op)});s.value=v;s.onchange=()=>fn(s.value);return s}
function num(v,fn){const i=mk('input');i.type='number';i.step='any';i.value=v;i.onchange=()=>fn(parseFloat(i.value));return i}
function condw(c){
 const w=mk('span','','cond');
 w.append(sel(SN,c.k,v=>{c.k=v;c.op=OPS[v][0];c.v=v=='color'?'red':20;draw()}),sel(OPS[c.k],c.op,v=>{c.op=v;save()}));
 if(c.k=='dist')w.append(num(c.v,v=>{c.v=v;save()}));
 if(c.k=='color')w.append(sel(COL,c.v,v=>{c.v=v;save()}));
 return w}
function nb(t){const b={t,a:{}};D[t].forEach(x=>{if(x.k)b.a[x.k]=x.d;if(x.cond)b.cond={k:'t1',op:'pressed',v:20}});if(BOX[t])b.c=[];return b}
function inside(b,L){return !!b.c&&(b.c===L||b.c.some(x=>inside(x,L)))}
function put(L,i){
 if(!drag)return;let b;
 if(drag.t)b=nb(drag.t);
 else{if(inside(drag.b,L))return;const j=drag.L.indexOf(drag.b);drag.L.splice(j,1);if(drag.L===L&&j<i)i--;b=drag.b}
 L.splice(i,0,b);drag=null;draw()}
function slot(L,i){
 const s=mk('div','','slot');
 s.ondragover=e=>{e.preventDefault();s.classList.add('on')};
 s.ondragleave=()=>s.classList.remove('on');
 s.ondrop=e=>{e.preventDefault();e.stopPropagation();put(L,i)};
 return s}
function list(L,cls){const d=mk('div','',cls);L.forEach((b,i)=>d.append(slot(L,i),block(b,L)));d.append(slot(L,L.length));return d}
function block(b,L){
 const e=mk('div','','blk'+(BOX[b.t]?' cb':'')),top=mk('div','','top');
 e.style.setProperty('--c',CC[b.t]);e.draggable=true;
 e.ondragstart=ev=>{ev.stopPropagation();drag={b,L};ev.dataTransfer.setData('text','b')};
 D[b.t].forEach(x=>{
  if(typeof x=='string')top.append(x);
  else if(x.cond)top.append(condw(b.cond));
  else top.append(x.o?sel(x.o,b.a[x.k],v=>{b.a[x.k]=v;save()}):num(b.a[x.k],v=>{b.a[x.k]=v;save()}))});
 const del=mk('button','\u2715','del');del.onclick=()=>{L.splice(L.indexOf(b),1);draw()};top.append(del);
 e.append(top);
 if(BOX[b.t])e.append(list(b.c,'kids'),mk('div','','bot'));
 return e}
function draw(){const r=$('prog');r.innerHTML='';r.append(mk('div','\u25B6 when Run clicked','hat'),list(prog));save()}
function pal(){
 $('cats').innerHTML='';
 CATS.forEach((c,i)=>{const b=mk('button',c.n,i==cur?'cat on':'cat');b.style.borderLeft='8px solid '+c.c;b.onclick=()=>{cur=i;pal()};$('cats').append(b)});
 const p=$('blocks');p.innerHTML='';
 CATS[cur].b.forEach(t=>{
  const e=mk('div','','blk'+(BOX[t]?' cb':'')),top=mk('div','','top');e.style.setProperty('--c',CC[t]);
  D[t].forEach(x=>top.append(typeof x=='string'?x:x.cond?mk('span','condition','cond'):mk('span',String(x.d),'pill')));
  e.append(top);if(BOX[t])e.append(mk('div','','kids'),mk('div','','bot'));
  e.draggable=true;e.ondragstart=ev=>{drag={t};ev.dataTransfer.setData('text','p')};
  e.onclick=()=>{prog.push(nb(t));draw()};p.append(e)})}
$('lt').ondragover=e=>e.preventDefault();
$('lt').ondrop=()=>{if(drag&&drag.b){drag.L.splice(drag.L.indexOf(drag.b),1);drag=null;draw()}};
async function post(u,b){const r=await fetch(u,{method:'POST',headers:{'X-Token':TOKEN,'Content-Type':'application/json'},body:JSON.stringify(b||{})});return r.json()}
$('run').onclick=()=>post('/run',prog).then(r=>{if(r.error)alert(r.error)});
$('stop').onclick=()=>post('/stop');
$('clr').onclick=()=>{prog=[];draw()};
$('ex').onclick=()=>{prog=[{t:'forever',a:{},c:[
 {t:'if',a:{},cond:{k:'t1',op:'pressed',v:20},c:[{t:'motor_on',a:{m:'A+C',p:60}}]},
 {t:'if',a:{},cond:{k:'t1',op:'not pressed',v:20},c:[{t:'motor_off',a:{m:'A+C'}}]}]}];draw()};
setInterval(async()=>{try{const s=await(await fetch('/status')).json();
 $('log').textContent=s.log.join('\n');$('st').textContent=s.running?'\u25CF running':'idle'}
 catch(e){$('st').textContent='server not reachable'}},500);
pal();draw();
</script></body></html>"""


if __name__ == "__main__":
    server = ThreadingHTTPServer(("127.0.0.1", PORT), Handler)
    url = "http://127.0.0.1:%d/" % PORT
    print("NXT Blocks v2 is running at", url, "- press Ctrl-C here to quit.")
    webbrowser.open(url)
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        pass
