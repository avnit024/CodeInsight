const $=id=>document.getElementById(id);
const esc=s=>String(s??'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
const token=()=>localStorage.getItem('token');
function setTheme(t){document.documentElement.dataset.theme=t;localStorage.setItem('theme',t)}
setTheme(localStorage.getItem('theme')||'light');
function toggleTheme(){setTheme(document.documentElement.dataset.theme==='dark'?'light':'dark');if(window.onTheme)window.onTheme()}
function logout(){localStorage.removeItem('token');localStorage.removeItem('user');location.href='login.html'}
async function api(url,method='GET',body){
  const r=await fetch(url,{method,headers:{'Content-Type':'application/json',...(token()?{Authorization:'Bearer '+token()}:{})},body:body?JSON.stringify(body):undefined});
  if(r.status===401&&!url.includes('/auth/')){logout();throw new Error('Session expired')}
  const d=await r.json().catch(()=>({}));if(!r.ok)throw new Error(d.error||'Request failed');return d}
function toast(msg,err){let c=$('toasts');if(!c){c=document.createElement('div');c.id='toasts';document.body.append(c)}
  const t=document.createElement('div');t.className='toast'+(err?' err':'');t.textContent=msg;c.append(t);setTimeout(()=>t.remove(),2800)}
function countUp(el,to,suf=''){let n=0;const step=Math.max(1,Math.ceil(to/25));const t=setInterval(()=>{n=Math.min(to,n+step);el.textContent=n+suf;if(n>=to)clearInterval(t)},25)}
/* wraps <main id="main"> in the sidebar + top bar layout */
function shell(active,title){
  if(!token()){location.href='login.html';return}
  const nav=[['dashboard','📊','Dashboard'],['problems','📚','Problems'],['profiles','🌐','Profiles']],name=localStorage.getItem('user')||'?';
  const w=document.createElement('div');w.className='shell';
  w.innerHTML=`<aside><div class="logo">&lt;/&gt; CodeInsight</div>${nav.map(([k,i,l])=>`<a href="${k}.html" class="${k===active?'active':''}"><span class="i">${i}</span> ${l}</a>`).join('')}<div class="acct" onclick="openSettings()" title="Account settings"><div class="avatar">${esc(name[0].toUpperCase())}</div><div class="who"><b>${esc(name)}</b><small>Account settings</small></div><span class="gear">⚙</span></div></aside>
  <div class="content"><header class="top"><div><h2>${title}</h2><div class="muted" id="hdrSub"></div></div>
  <button class="srch" onclick="openPalette()">🔍 Search or jump to... <kbd>Ctrl K</kbd></button>
  <div class="r"><span class="chip" id="hdrStreak" title="Current streak">🔥 -</span>
  <button class="ghost sm bell" title="Reviews due" onclick="location.href='dashboard.html'">🔔<b id="hdrDue" class="dot hidden"></b></button>
  <button class="ghost sm" title="Toggle theme" onclick="toggleTheme()">🌓</button>
  <button class="ghost sm mob" title="Account settings" onclick="openSettings()">⚙</button></div></header></div>`;
  document.body.prepend(w);w.querySelector('.content').append($('main'));
  const tick=()=>{const d=new Date(),h=d.getHours();$('hdrSub').textContent=`${h<12?'Good morning':h<18?'Good afternoon':'Good evening'}, ${name} · ${d.toLocaleDateString(undefined,{weekday:'short',day:'numeric',month:'short'})} · ${d.toLocaleTimeString([],{hour:'2-digit',minute:'2-digit'})}`};
  tick();setInterval(tick,30000);
  
  api('/api/stats').then(s=>$('hdrStreak').textContent='🔥 '+s.currentStreak+' day streak').catch(()=>{});
  api('/api/problems/due').then(d=>{if(d.length){$('hdrDue').textContent=d.length;$('hdrDue').classList.remove('hidden')}}).catch(()=>{})}

/* ===== v2.2 interactive effects ===== */
function bg(){ // particle network that reacts to the pointer
 if(matchMedia('(prefers-reduced-motion:reduce)').matches||localStorage.getItem('bgoff'))return;
 const c=document.createElement('canvas');c.id='bg';document.body.prepend(c);const x=c.getContext('2d');let W,H,P=[];const m={x:-999,y:-999};
 const rs=()=>{W=c.width=innerWidth;H=c.height=innerHeight;P=Array.from({length:Math.min(90,Math.floor(W*H/18000))},()=>({x:Math.random()*W,y:Math.random()*H,vx:(Math.random()-.5)*.4,vy:(Math.random()-.5)*.4}))};
 rs();addEventListener('resize',rs);addEventListener('mousemove',e=>{m.x=e.clientX;m.y=e.clientY});
 (function f(){if(!c.isConnected)return;x.clearRect(0,0,W,H);const col=window.__rgb||'99,102,241';
  P.forEach((p,i)=>{p.x+=p.vx;p.y+=p.vy;if(p.x<0||p.x>W)p.vx*=-1;if(p.y<0||p.y>H)p.vy*=-1;
   const dx=p.x-m.x,dy=p.y-m.y,d=Math.hypot(dx,dy);if(d<140&&d>0){p.x+=dx/d*1.2;p.y+=dy/d*1.2}
   x.fillStyle=`rgba(${col},.7)`;x.beginPath();x.arc(p.x,p.y,2,0,6.3);x.fill();
   for(let j=i+1;j<P.length;j++){const q=P[j],e=Math.hypot(p.x-q.x,p.y-q.y);if(e<120){x.strokeStyle=`rgba(${col},${.25*(1-e/120)})`;x.beginPath();x.moveTo(p.x,p.y);x.lineTo(q.x,q.y);x.stroke()}}
   if(d<170){x.strokeStyle=`rgba(${window.__rgb2||'236,72,153'},${.55*(1-d/170)})`;x.beginPath();x.moveTo(p.x,p.y);x.lineTo(m.x,m.y);x.stroke()}});
  requestAnimationFrame(f)})()}
function pointerFx(){ // cursor glow, card spotlight, 3D tilt
 const g=document.createElement('div');g.id='glow';document.body.prepend(g);
 addEventListener('mousemove',e=>{g.style.transform=`translate(${e.clientX}px,${e.clientY}px)`;
  const card=e.target.closest&&e.target.closest('.card');
  if(card){const r=card.getBoundingClientRect();card.style.setProperty('--mx',e.clientX-r.left+'px');card.style.setProperty('--my',e.clientY-r.top+'px')}
  const h=e.target.closest&&e.target.closest('.hover');
  document.querySelectorAll('.hover').forEach(el=>{if(el!==h)el.style.transform=''});
  if(h){const r=h.getBoundingClientRect(),px=(e.clientX-r.left)/r.width-.5,py=(e.clientY-r.top)/r.height-.5;
   h.style.transform=`perspective(700px) rotateX(${-py*8}deg) rotateY(${px*8}deg) translateY(-3px)`}})}
function confetti(){const c=document.createElement('canvas');c.style.cssText='position:fixed;inset:0;pointer-events:none;z-index:200';c.width=innerWidth;c.height=innerHeight;document.body.append(c);
 const x=c.getContext('2d'),P=Array.from({length:140},()=>({x:innerWidth/2,y:innerHeight/3,vx:(Math.random()-.5)*14,vy:Math.random()*-12-3,r:Math.random()*6+3,c:`hsl(${Math.random()*360},85%,60%)`}));let t=0;
 (function f(){x.clearRect(0,0,c.width,c.height);P.forEach(p=>{p.vy+=.35;p.x+=p.vx;p.y+=p.vy;x.fillStyle=p.c;x.fillRect(p.x,p.y,p.r,p.r)});if(++t<120)requestAnimationFrame(f);else c.remove()})()}
function palette(){ // Ctrl/Cmd+K command palette
 if(!token())return;const go=u=>()=>location.href=u;
 const cmds=[['📊 Go to Dashboard',go('dashboard.html')],['📚 Go to Problems',go('problems.html')],['🌐 Go to Profiles',go('profiles.html')],['➕ Add problem',go('problems.html?add=1')],['🌓 Toggle theme',toggleTheme],['🚪 Logout',logout]];
 const o=document.createElement('div');o.className='modal hidden';o.innerHTML='<div class="box"><input id="pq" placeholder="Type a command..." style="width:100%;margin-bottom:8px"><div id="pl"></div><p class="muted">↑↓ to move · Enter to run · Esc to close</p></div>';document.body.append(o);
 let sel=0,list=cmds;const draw=()=>{$('pl').innerHTML=list.map((c,i)=>`<div class="cmd ${i===sel?'on':''}" data-i="${i}">${c[0]}</div>`).join('')};
 const open=()=>{o.classList.remove('hidden');$('pq').value='';list=cmds;sel=0;draw();$('pq').focus()},run=i=>{o.classList.add('hidden');list[i]&&list[i][1]()};
 addEventListener('keydown',e=>{if((e.ctrlKey||e.metaKey)&&e.key.toLowerCase()==='k'){e.preventDefault();open()}
  else if(e.key==='Escape')o.classList.add('hidden');
  else if(!o.classList.contains('hidden')){if(e.key==='ArrowDown'){sel=(sel+1)%list.length;draw()}else if(e.key==='ArrowUp'){sel=(sel-1+list.length)%list.length;draw()}else if(e.key==='Enter')run(sel)}});
 o.addEventListener('input',()=>{list=cmds.filter(c=>c[0].toLowerCase().includes($('pq').value.toLowerCase()));sel=0;draw()});
 o.addEventListener('click',e=>{const d=e.target.closest('.cmd');if(d)run(+d.dataset.i);else if(e.target===o)o.classList.add('hidden')});
 window.openPalette=open}
document.addEventListener('keydown',e=>{ // keyboard shortcuts
 if(!token()||e.ctrlKey||e.metaKey||e.altKey||/INPUT|TEXTAREA|SELECT/.test(e.target.tagName))return;
 if(e.key==='n')location.href='problems.html?add=1';
 else if(e.key==='/'){e.preventDefault();const q=$('q');q?q.focus():location.href='problems.html'}
 else if(e.key==='b'&&window.toggleView)toggleView();
 else if(e.key==='?')toast('N new · / search · B board view · Ctrl+K palette')});
document.addEventListener('DOMContentLoaded',()=>{bg();pointerFx();palette()});

/* ===== v3.1 accent colors, account settings, ripple ===== */
const PRESETS=[['Indigo','#6366f1','#ec4899'],['Ocean','#0ea5e9','#6366f1'],['Emerald','#10b981','#06b6d4'],['Sunset','#f97316','#ec4899'],['Violet','#8b5cf6','#d946ef'],['Rose','#f43f5e','#f59e0b']];
const hexRgb=h=>[1,3,5].map(i=>parseInt(h.slice(i,i+2),16)).join(',');
function setAccent(i){const p=PRESETS[i]||PRESETS[0],r=document.documentElement.style;r.setProperty('--a1',p[1]);r.setProperty('--a2',p[2]);window.__rgb=hexRgb(p[1]);window.__rgb2=hexRgb(p[2]);localStorage.setItem('accent',i)}
setAccent(+localStorage.getItem('accent')||0);
function openSettings(){
 const name=localStorage.getItem('user')||'',oauth=name.startsWith('github_'),acc=+localStorage.getItem('accent')||0,ini=esc((name[0]||'?').toUpperCase());
 let g=+localStorage.getItem('goal')||3;
 const old=$('settings');if(old)old.remove();
 const m=document.createElement('div');m.id='settings';m.className='modal';
 m.innerHTML=`<div class="box set2"><button class="ghost sm sclose" id="sx" title="Close (Esc)">✕</button>
 <div class="snav"><div class="sprof"><div class="avatar big">${ini}</div><b>${esc(name)}</b><small>${oauth?'GitHub account':'Password account'}</small></div>
  <button class="tab ghost on" data-t="p">👤 Profile</button><button class="tab ghost" data-t="a">🎨 Appearance</button><button class="tab ghost" data-t="g">🎯 Goals</button>
  <button class="tab ghost" data-t="s">🔒 Security</button><button class="tab ghost dng" data-t="d">⚠ Danger zone</button><button class="tab ghost lo" onclick="logout()">🚪 Logout</button></div>
 <div class="spanes">
  <section class="pane" id="t_p"><h3>Profile</h3><div class="pcard"><div class="avatar big">${ini}</div><div><h2 style="margin:0">${esc(name)}</h2><span class="badge Solved">${oauth?'Signed in with GitHub':'Password account'}</span></div></div>
   <p class="muted">Your progress</p><div class="minis" id="ps"><div class="mini"><b>-</b><span>Loading</span></div></div></section>
  <section class="pane hidden" id="t_a"><h3>Appearance</h3><p class="muted">Accent color</p>
   <div class="sw">${PRESETS.map((p,i)=>`<div class="swi"><button class="swatch ${i===acc?'on':''}" data-i="${i}" style="background:linear-gradient(135deg,${p[1]},${p[2]})"></button><small>${p[0]}</small></div>`).join('')}</div>
   <div class="trow"><div><b>Dark mode</b><div class="muted">Easier on the eyes at night</div></div><label class="switch"><input type="checkbox" id="sdark" ${document.documentElement.dataset.theme==='dark'?'checked':''}><span></span></label></div>
   <div class="trow"><div><b>Particle background</b><div class="muted">Turn off to save battery</div></div><label class="switch"><input type="checkbox" id="sbg" ${localStorage.getItem('bgoff')?'':'checked'}><span></span></label></div></section>
  <section class="pane hidden" id="t_g"><h3>Daily goal</h3><p class="muted">How many problems do you want to solve each day?</p>
   <div class="stepper"><button class="ghost" id="gminus">−</button><div><b id="gval">${g}</b><span class="muted">per day</span></div><button class="ghost" id="gplus">+</button></div>
   <button id="gsave">Save goal</button></section>
  <section class="pane hidden" id="t_s"><h3>Security</h3>${oauth?'<p class="muted">You signed in with GitHub, so there is no password to change here.</p>':`
   <input type="password" id="pcur" placeholder="Current password"><input type="password" id="pnew" placeholder="New password"><div class="meter"><span id="pbar"></span></div><small class="muted" id="plab">Use 8+ characters with a mix of letters, numbers and symbols</small>
   <input type="password" id="pcon" placeholder="Confirm new password" style="margin-top:10px"><div class="row" style="margin-top:6px"><button id="pbtn">Update password</button><button class="ghost sm" id="peye">👁 Show / hide</button></div>`}<div class="err" id="perr"></div></section>
  <section class="pane hidden" id="t_d"><h3 style="color:#ef4444">Danger zone</h3><div class="dzone"><p><b>Delete account</b></p><p class="muted">This permanently removes your account and every problem, streak and review you saved. It cannot be undone.</p>
   <input id="dconf" placeholder="Type ${esc(name)} to confirm"><button class="dan" id="dbtn" disabled>Delete my account</button><div class="err" id="derr"></div></div></section>
 </div></div>`;
 document.body.append(m);
 const onKey=e=>{if(!m.isConnected){document.removeEventListener('keydown',onKey)}else if(e.key==='Escape'){m.remove()}};document.addEventListener('keydown',onKey);
 m.onclick=e=>{if(e.target===m||e.target.id==='sx')m.remove()};
 m.querySelectorAll('.tab[data-t]').forEach(b=>b.onclick=()=>{m.querySelectorAll('.tab').forEach(x=>x.classList.toggle('on',x===b));m.querySelectorAll('.pane').forEach(p=>p.classList.toggle('hidden',p.id!=='t_'+b.dataset.t))});
 api('/api/stats').then(s=>{const b=s.byDifficulty,xp=(b.Easy||0)*10+(b.Medium||0)*20+(b.Hard||0)*40,lv=Math.floor(Math.sqrt(xp/50))+1;
  $('ps').innerHTML=[[s.solved,'Solved'],[s.currentStreak+' 🔥','Streak'],[s.longestStreak,'Best streak'],['Lv '+lv,'Level']].map(([v,l])=>`<div class="mini"><b>${v}</b><span>${l}</span></div>`).join('')}).catch(()=>{});
 m.querySelectorAll('.swatch').forEach(b=>b.onclick=()=>{setAccent(+b.dataset.i);m.querySelectorAll('.swatch').forEach(x=>x.classList.toggle('on',x===b));if(window.onTheme)window.onTheme()});
 $('sdark').onchange=e=>{setTheme(e.target.checked?'dark':'light');if(window.onTheme)window.onTheme()};
 $('sbg').onchange=e=>{if(e.target.checked){localStorage.removeItem('bgoff');bg()}else{localStorage.setItem('bgoff',1);const c=$('bg');c&&c.remove()}};
 $('gminus').onclick=()=>{g=Math.max(1,g-1);$('gval').textContent=g};$('gplus').onclick=()=>{g=Math.min(50,g+1);$('gval').textContent=g};
 $('gsave').onclick=()=>{localStorage.setItem('goal',g);toast('Daily goal set to '+g);if(window.onTheme)window.onTheme()};
 if($('pbtn')){
  const pw=['pcur','pnew','pcon'];
  $('pnew').oninput=()=>{const v=$('pnew').value,n=[v.length>=8,/[A-Z]/.test(v),/[0-9]/.test(v),/[^A-Za-z0-9]/.test(v)].filter(Boolean).length,L=['Too weak','Weak','Fair','Good','Strong'],C=['#ef4444','#ef4444','#f59e0b','#22c55e','#16a34a'];
   $('pbar').style.width=(v?Math.max(n,1)*25:0)+'%';$('pbar').style.background=C[n];$('plab').textContent=v?L[n]:'Use 8+ characters with a mix of letters, numbers and symbols'};
  $('peye').onclick=()=>pw.forEach(i=>$(i).type=$(i).type==='password'?'text':'password');
  $('pbtn').onclick=async()=>{$('perr').textContent='';
   if($('pnew').value!==$('pcon').value){$('perr').textContent='New passwords do not match';return}
   try{await api('/api/account/password','POST',{currentPassword:$('pcur').value,newPassword:$('pnew').value});pw.forEach(i=>$(i).value='');$('pbar').style.width='0';toast('Password updated')}catch(e){$('perr').textContent=e.message}}}
 $('dconf').oninput=()=>{$('dbtn').disabled=$('dconf').value!==name};
 $('dbtn').onclick=async()=>{$('derr').textContent='';if(!confirm('Permanently delete your account and all problems?'))return;
  try{await api('/api/account/delete','POST',{confirm:name});logout()}catch(e){$('derr').textContent=e.message}}}
document.addEventListener('click',e=>{const b=e.target.closest&&e.target.closest('button');if(!b||b.classList.contains('swatch'))return;
 const r=b.getBoundingClientRect(),s=document.createElement('span'),d=Math.max(r.width,r.height);s.className='rip';
 s.style.cssText=`width:${d}px;height:${d}px;left:${e.clientX-r.left-d/2}px;top:${e.clientY-r.top-d/2}px`;b.append(s);setTimeout(()=>s.remove(),600)});
