function renderAtlas(stage="adult",sex="boy"){
  const N=96,W=N*6,H=N*4, data=new Uint8Array(W*H);
  const colors=["000000","241c2a","503040","823d31","b84d22","e46b22","ff9635","ffc05b","fff2d1","e2bfa9","ab857d","172a3d","294658","446878","7292a0","086878","1db6cf","72f3f5","e9ffff","141925","6b323a","e97c8d","fff9ed","9d5a50","d99058","ccf9ff","71b7d1","34758b","424354","af5d43","ffe09a","c13f40"];
  const ultra=stage==="ultra",girl=sex==="girl";
  for(let f=0;f<24;f++){
    const ox=(f%6)*N,oy=Math.floor(f/6)*N;
    const p=(x,y,c)=>{x=Math.round(48+(x-48)*.88);y=Math.round(88+(y-88)*.88);if(x>=0&&x<N&&y>=0&&y<N)data[(oy+y)*W+ox+x]=c};
    const line=(x0,y0,x1,y1,c,w=1)=>{x0=Math.round(x0);y0=Math.round(y0);x1=Math.round(x1);y1=Math.round(y1);let dx=Math.abs(x1-x0),dy=-Math.abs(y1-y0),sx=x0<x1?1:-1,sy=y0<y1?1:-1,e=dx+dy;for(;;){for(let j=0;j<w;j++)for(let i=0;i<w;i++)p(x0+i-Math.floor(w/2),y0+j-Math.floor(w/2),c);if(x0===x1&&y0===y1)break;const z=2*e;if(z>=dy){e+=dy;x0+=sx}if(z<=dx){e+=dx;y0+=sy}}};
    const poly=(pts,c,outline=1)=>{let minX=Math.floor(Math.min(...pts.map(a=>a[0]))),maxX=Math.ceil(Math.max(...pts.map(a=>a[0]))),minY=Math.floor(Math.min(...pts.map(a=>a[1]))),maxY=Math.ceil(Math.max(...pts.map(a=>a[1])));for(let y=minY;y<=maxY;y++)for(let x=minX;x<=maxX;x++){let hit=false;for(let i=0,j=pts.length-1;i<pts.length;j=i++){const a=pts[i],b=pts[j];if((a[1]>y+.25)!=(b[1]>y+.25)&&x+.25<(b[0]-a[0])*(y+.25-a[1])/(b[1]-a[1])+a[0])hit=!hit}if(hit)p(x,y,c)}if(outline!==null)for(let i=0;i<pts.length;i++)line(...pts[i],...pts[(i+1)%pts.length],outline)};
    const ell=(cx,cy,rx,ry,c,edge=null)=>{for(let y=Math.floor(cy-ry);y<=Math.ceil(cy+ry);y++)for(let x=Math.floor(cx-rx);x<=Math.ceil(cx+rx);x++){const q=((x-cx)/rx)**2+((y-cy)/ry)**2;if(q<=1)p(x,y,edge!==null&&q>.76?edge:c)}};
    const patch=(x,y,points,c,e=null)=>poly(points.map(([a,b])=>[x+a,y+b]),c,e);
    const stroke=(x,y,points,c,w=1)=>{for(let i=1;i<points.length;i++)line(x+points[i-1][0],y+points[i-1][1],x+points[i][0],y+points[i][1],c,w)};
    const main=girl?6:5, light=girl?7:6, shade=girl?5:4;
    function crystal(x,y,s=1){patch(x,y,[[0,-6*s],[4*s,-1*s],[2*s,5*s],[-3*s,3*s],[-4*s,-1*s]],16,11);line(x,y-5*s,x,y+2*s,25);line(x,y-3*s,x+3*s,y,17)}
    function tail(x,y,mode=0){
      const pts=mode===2?[[0,0],[-12,-6],[-22,-6],[-32,0],[-39,9],[-36,19],[-26,23],[-11,20],[0,12],[7,5]]:[[0,0],[-9,-1],[-16,-8],[-19,-20],[-24,-29],[-32,-31],[-39,-27],[-45,-17],[-44,-7],[-39,2],[-31,9],[-20,13],[-9,12],[6,7]];
      const a=pts.map(([u,v])=>[x+u,y+v+(mode===1?Math.round((-u/45)*5):mode===3?Math.round((u/45)*4):0)]);
      poly(a,main,1);
      if(mode===2){patch(x,y,[[-39,9],[-36,19],[-26,23],[-11,20],[-7,15],[-16,15],[-20,10],[-25,13],[-30,8],[-34,11]],8);patch(x,y,[[-35,17],[-26,21],[-14,19],[-18,16],[-26,17]],9)}
      else {
        const shift=mode===1?4:mode===3?-4:0;
        patch(x,y,[[-24,-29+shift],[-32,-31+shift],[-39,-27+shift],[-45,-17+shift],[-44,-7+shift],[-38,-3+shift],[-34,-6+shift],[-32,0+shift],[-27,-6+shift],[-25,-2+shift],[-22,-9+shift],[-21,-17+shift]],8);
        patch(x,y,[[-44,-12+shift],[-41,-5+shift],[-34,-3+shift],[-33,-8+shift],[-39,-11+shift]],9);
        patch(x,y,[[-30,9],[-20,13],[-9,12],[4,7],[-3,3],[-13,6],[-22,5]],shade);
        stroke(x,y,[[-27,2],[-22,-2],[-16,1],[-12,5]],15,2);stroke(x,y,[[-27,1],[-22,-3],[-16,0],[-12,4]],17);
        if(ultra){crystal(x-21,y-5);crystal(x-13,y+1,.85);crystal(x-29,y-11,.8);crystal(x-17,y-12,.7);stroke(x,y,[[-36,0],[-30,4],[-22,6]],17,2)}
      }
    }
    function leg(ax,ay,kx,ky,fx,fy,near=true){
      let col=near?main:shade,edge=near?1:2;
      poly([[ax-5,ay-4],[ax+5,ay-4],[kx+5,ky],[fx+4,fy-5],[fx+8,fy-3],[fx+8,fy],[fx-5,fy],[fx-7,fy-3],[kx-5,ky+2],[ax-6,ay+5]],col,edge);
      poly([[kx-4,ky+1],[kx+4,ky],[fx+4,fy-5],[fx+7,fy-3],[fx+7,fy-1],[fx-5,fy-1],[fx-6,fy-3]],near?8:9,null);
      line(fx+1,fy-3,fx+1,fy-1,10);line(fx+4,fy-3,fx+4,fy-1,10);
      if(near){line(kx-3,ky-5,kx+3,ky-4,11,2);line(kx-2,ky-4,kx+2,ky-3,16);if(ultra)line(kx-3,ky-2,kx+2,ky-1,17)}
    }
    function ear(x,y,far=false,front=false){
      const tilt=girl?3:0;
      patch(x,y,[[-9,8],[-12+tilt,-5],[-10+tilt,girl?-21:-19],[-5,girl?-18:-16],[4,-8],[10,7],[4,11]],far?shade:main,1);
      patch(x,y,[[-8+tilt,-13],[-6+tilt,-7],[-5,2],[0,5],[5,5],[2,-4]],9,3);
      patch(x,y,[[-7+tilt,-12],[-5,-8],[-3,-1],[0,1]],21,null);
      if(ultra)patch(x,y,[[-11,-18],[-7,-23],[-2,-13],[-5,-10]],16,11);
    }
    function eye(x,y,mode,far=false,front=false){
      if(mode==="closed"||mode==="happy"){stroke(x,y,[[-5,mode==="happy"?1:-1],[-2,-2],[2,-2],[5,mode==="happy"?1:-1]],19,2);return}
      if(mode==="half"||mode==="tired"){ell(x,y+1,5,3,8,1);ell(x+1,y+1,3,2,16);line(x-5,y-1,x+5,y-1,1,2);p(x+1,y,18);return}
      const rx=far?4:6,ry=mode==="surprise"?7:5;
      ell(x,y,rx,ry,8,1);ell(x+(front?0:2),y,far?2.7:4,ry-1,15);ell(x+(front?0:2),y+1,far?2:3,ry-2,16);ell(x+(front?0:2),y,far?1.5:2.2,ry-2,19);ell(x+(front?-1:1),y-2,1.5,1.5,18);p(x+3,y+2,17);
      if(mode==="sad")stroke(x,y,[[-rx,-ry-1],[0,-ry-3],[rx,-ry-1]],3,2);else if(mode!=="surprise")stroke(x,y,[[-rx,-ry-1],[1,-ry-2],[rx,-ry]],1);
      if(girl&&!far){line(x-rx,y-ry+1,x-rx-2,y-ry-1,1);line(x-rx+1,y-ry,x-rx,y-ry-2,1);}
    }
    function headset(x,y,side=true){ell(x,y,side?8:6,9,11,1);ell(x,y,side?6:4,7,13,12);ell(x,y,side?4:2.7,5,15,16);line(x-2,y-4,x-2,y+3,17);if(ultra){p(x+3,y-4,25);p(x+3,y+4,25)}}
    function sideHead(x,y,mode="open",low=false){
      ear(x+10,y-11,true);ear(x-6,y-10);
      patch(x,y,[[-17,-5],[-11,-13],[-2,-16],[9,-14],[15,-10],[19,-4],[21,3],[28,8],[27,15],[20,18],[10,17],[6,21],[-4,17],[-12,15],[-18,8]],main,1);
      patch(x,y,[[-16,-5],[-10,-11],[-2,-13],[7,-11],[11,-6],[4,-6],[0,-9],[-4,-5],[-8,-4]],light);
      patch(x,y,[[-13,6],[-7,10],[-2,8],[4,11],[10,10],[16,4],[22,5],[28,8],[27,15],[20,18],[10,17],[6,21],[-4,17],[-12,15]],8);
      patch(x,y,[[-11,13],[-3,16],[7,19],[13,16],[23,16],[27,13],[24,17],[20,18],[10,17],[6,21],[-4,17],[-12,15]],9);
      patch(x,y,[[-3,-15],[6,-15],[3,-9],[9,-12],[8,-5],[3,-1],[0,-5],[-4,-2]],light,3);
      stroke(x,y,[[-15,-9],[-10,-15],[-1,-17],[6,-16]],11,3);stroke(x,y,[[-14,-9],[-9,-14],[-2,-16]],14);
      headset(x-12,y+2);
      eye(x+10,y+1,mode,false,false);
      poly([[x+25,y+7],[x+28,y+8],[x+28,y+11],[x+25,y+12],[x+23,y+10]],19,1);
      if(mode==="happy"||mode==="surprise"||mode==="chew"||mode==="drink"){ell(x+19,y+14,4,3,20,1);if(mode==="happy"||mode==="chew")line(x+17,y+15,x+21,y+15,21)}
      else stroke(x,y,[[16,14],[20,15],[24,13]],3);
      if(ultra){crystal(x+3,y-12,.8);line(x-2,y-5,x+3,y-8,17,2);patch(x,y,[[-5,-16],[-2,-23],[4,-17],[8,-21],[11,-13],[5,-10]],16,11);line(x-2,y-20,x+2,y-15,25)}
    }
    function frontHead(x,y,mode){
      ear(x-14,y-11);ear(x+15,y-11,true);
      patch(x,y,[[girl?-23:-24,-5],[-20,-13],[-10,-17],[0,-18],[12,-16],[21,-11],[girl?23:25,-2],[girl?21:23,9],[15,18],[6,21],[-6,21],[-16,17],[girl?-22:-24,8]],main,1);
      patch(x,y,[[-21,4],[-14,8],[-8,7],[-4,11],[0,10],[5,11],[9,7],[16,8],[23,3],[23,9],[16,18],[6,21],[-6,21],[-17,17],[-24,8]],8);
      patch(x,y,[[-16,15],[-6,19],[4,19],[16,15],[12,19],[5,21],[-6,21],[-16,17]],9);
      patch(x,y,[[-9,-16],[4,-18],[2,-12],[9,-14],[6,-6],[1,-2],[-3,-7],[-7,-3],[-11,-9]],light,3);
      stroke(x,y,[[-23,-4],[-20,-12],[-11,-17],[-5,-18]],11,3);stroke(x,y,[[6,-18],[14,-15],[21,-10],[24,-4]],11,3);
      headset(x-23,y+1,false);headset(x+24,y+1,false);
      eye(x-11,y+2,mode,false,true);eye(x+11,y+2,mode,false,true);
      patch(x,y,[[-3,9],[3,9],[2,12],[0,13],[-2,12]],19,1);
      if(mode==="happy"||mode==="surprise"||mode==="chew"||mode==="drink"){ell(x,y+16,mode==="surprise"?3:5,mode==="surprise"?4:3,20,1);if(mode==="happy"||mode==="chew")line(x-2,y+17,x+2,y+17,21)}
      else if(mode==="sad")stroke(x,y,[[-5,17],[-2,14],[2,14],[5,17]],3);
      else stroke(x,y,[[-5,14],[-2,16],[0,15],[2,16],[5,14]],3);
      if(girl&&!ultra){patch(x,y,[[4,-14],[9,-14],[7,-8],[3,-10]],7,3);p(x+7,y-9,17)}
      if(ultra){patch(x,y,[[-9,-16],[-5,-23],[0,-17],[5,-23],[10,-16],[5,-11],[-5,-11]],16,11);crystal(x,y-13,.8);line(x-5,y-6,x,y-9,17);line(x,y-9,x+5,y-6,17)}
      if(girl&&mode==="happy"){line(x-15,y+10,x-12,y+11,21);line(x+12,y+11,x+15,y+10,21)}
    }
    function armor(x,y,front=false){
      if(front){patch(x,y,[[-15,-7],[-9,-10],[0,-6],[9,-10],[15,-7],[12,4],[0,11],[-12,4]],11,1);patch(x,y,[[-11,-5],[0,0],[11,-5],[7,3],[0,8],[-7,3]],13,12);patch(x,y,[[-5,0],[5,0],[0,6]],16,11);line(x-3,y+1,x+3,y+1,17);if(ultra){crystal(x-13,y-2,.7);crystal(x+13,y-2,.7)}}
      else {patch(x,y,[[-10,-7],[-1,-9],[8,-5],[11,0],[5,9],[-5,6],[-11,1]],11,1);patch(x,y,[[-6,-5],[2,-6],[7,-2],[5,4],[-2,5],[-7,0]],13,12);patch(x,y,[[-3,-2],[4,-2],[1,3]],16,11);if(ultra){crystal(x-7,y-7,.65);crystal(x+6,y-5,.7)}}
    }
    function sidePose(kind,i){
      const walk=kind==="walk",hop=kind==="hop",sniff=kind==="sniff";
      const yb=hop?[0,-5,-9,-3][i]:sniff?[0,1,2,1][i]:[0,-1,0,-1][i];
      const headDrop=sniff?[4,13,18,10][i]:hop?[0,-1,-1,1][i]:0;
      const foot=88,bodyY=(girl?62:61)+yb,bodyX=48;
      const fx=girl?-1:0;
      tail(48,bodyY-3,walk?i%2:sniff?3:hop?1:0);
      const gait=[[-4,4,4,-4],[4,-4,-4,4],[2,-2,-2,2],[-2,2,2,-2]][i];
      const feet=hop?[[0,0,0,0],[3,-3,-4,5],[5,-3,-6,4],[-2,2,1,-1]][i]:walk?gait:[0,0,0,0];
      const lifts=hop?[0,5,11,3][i]:0;
      const h1=walk&&i===1?4:walk&&i===3?2:0,h2=walk&&i===0?3:walk&&i===2?3:0;
      leg(40,bodyY+1,37+feet[0],73+yb,35+feet[0],foot-lifts-h1,false);
      leg(63,bodyY+1,62+feet[1],74+yb,62+feet[1],foot-lifts-h2,false);
      patch(bodyX,bodyY,[[-18,-11],[-9,girl?-13:-16],[5,girl?-12:-15],[18,-12],[22,-5],[16,6],[6,12],[-8,10],[-18,4],[-21,-3]],main,1);
      patch(bodyX,bodyY,[[-17,-9],[-7,-13],[8,-13],[17,-10],[9,-5],[-7,-6],[-15,-2]],light);
      patch(bodyX,bodyY,[[-18,1],[-10,7],[5,10],[16,5],[19,-1],[12,10],[6,12],[-8,10],[-18,4]],shade);
      if(ultra){patch(bodyX,bodyY,[[-10,-15],[-4,-21],[2,-17],[8,-21],[13,-13],[7,-8],[-3,-9]],11,1);crystal(bodyX-4,bodyY-15,.75);crystal(bodyX+8,bodyY-12,.6)}else{stroke(bodyX,bodyY,[[-13,-11],[-5,-13],[5,-12],[13,-9]],12,3);line(bodyX-7,bodyY-12,bodyX+6,bodyY-11,16);patch(bodyX,bodyY,[[-13,-5],[-5,-7],[-1,-4],[-6,0],[-12,0]],6);line(bodyX-12,bodyY-2,bodyX-8,bodyY+1,4)}
      leg(36,bodyY+3,34+feet[2],74+yb,35+feet[2],foot-lifts-h2,true);
      leg(62,bodyY+4,62+feet[3],74+yb,64+feet[3],foot-lifts-h1,true);
      const hx=62+fx,hy=(girl?34:33)+yb+headDrop;
      patch(hx-4,hy+21,[[-13,-3],[-8,-9],[6,-8],[10,0],[7,8],[3,13],[0,9],[-5,12],[-9,6]],8,1);
      armor(hx-6,hy+24);
      sideHead(hx,hy,sniff&&i===2?"half":"open",sniff);
    }
    function seated(mode,prop){
      const x=girl?49:48,y=35;
      tail(49,70,0);
      patch(x,70,[[-20,11],[-20,-3],[-15,-15],[-9,-21],[9,-21],[15,-15],[20,-3],[20,11],[13,17],[-13,17]],main,1);
      patch(x,70,[[-13,-10],[0,-14],[13,-10],[8,8],[0,13],[-8,8]],8,3);
      for(const k of [-1,1]){
        patch(x+k*12,77,[[-6,-14],[5,-14],[6,3],[9,7],[7,11],[-6,11],[-9,7],[-6,2]],main,1);
        patch(x+k*12,77,[[-6,2],[6,2],[8,7],[6,10],[-6,10],[-8,7]],8);
        line(x+k*12-2,85,x+k*12-2,87,10);line(x+k*12+2,85,x+k*12+2,87,10);
      }
      armor(x,59,true);
      frontHead(x,y,mode);
      if(prop){
        // Draw a complete care pose: rear paws stay planted, forelegs reach the object.
        for(const k of [-1,1]){
          patch(x+k*12,77,[[-7,-15],[7,-15],[8,8],[5,10],[-5,10],[-8,7]],main,1);
          patch(x+k*12,77,[[-7,5],[7,5],[7,9],[-7,9]],8);
          poly([[x+k*14,66],[x+k*16,69],[x+k*10,76],[x+k*5,65],[x+k*7,61],[x+k*12,64]],main,1);
        }
        if(prop==="cookie"){
          ell(x,57,6,5,24,3);
          for(const [a,b] of [[-2,-2],[2,1],[-3,2],[3,-2]])p(x+a,57+b,3);
          if(f===22){ell(x+3,53,3,2,8);line(x-4,57,x+3,59,3);}
        }else{
          patch(x,60,[[-6,-6],[6,-6],[5,4],[3,6],[-3,6],[-5,4]],16,11);
          line(x-5,54,x+5,54,25,2);line(x-3,56,x-3,62,17);
        }
        for(const k of [-1,1])ell(x+k*7,62,3,3,8,3);
      }
    }
    function sleep(){
      tail(52,64,2);
      patch(53,72,[[-26,3],[-24,-8],[-17,-17],[-3,-20],[11,-17],[23,-10],[26,-1],[20,8],[8,13],[-12,13],[-25,9]],main,1);
      patch(53,72,[[-20,-9],[-10,-15],[4,-16],[15,-12],[10,-7],[-2,-5],[-14,-4]],light);
      sideHead(63,59,"closed");
      patch(62,83,[[-17,-3],[-9,-7],[-1,-5],[5,-4],[7,0],[4,3],[-17,3]],8,1);
      line(62,83,62,85,10);
      // Tail in front of the body, curled around the paws.
      tail(50,65,2);
    }
    if(f<4)sidePose("walk",f);
    else if(f<8)sidePose("sniff",f-4);
    else if(f<12)sidePose("hop",f-8);
    else if(f===21)sleep();
    else {const modes={12:"open",13:"half",14:"closed",15:"happy",16:"sad",17:"tired",18:"surprise",19:"chew",20:"drink",22:"closed",23:"half"};seated(modes[f],f===19||f===22?"cookie":f===20||f===23?"cup":null)}
  }
  return {W,H,data,colors};
}
function png(W,H,data,colors){
 const arr=[];const put32=(a,v)=>{a.push((v>>>24)&255,(v>>>16)&255,(v>>>8)&255,v&255)};
 const crc=(a)=>{let c=-1;for(const b of a){c^=b;for(let k=0;k<8;k++)c=(c>>>1)^((c&1)?0xedb88320:0)}return (c^-1)>>>0};
 const chunk=(name,bytes)=>{put32(arr,bytes.length);const b=[...name].map(c=>c.charCodeAt(0));for(const v of b)arr.push(v);for(const v of bytes)arr.push(v);put32(arr,crc(b.concat(Array.from(bytes))))};
 arr.push(137,80,78,71,13,10,26,10);let h=[];put32(h,W);put32(h,H);h.push(8,3,0,0,0);chunk("IHDR",h);
 const pal=[];for(const c of colors){pal.push(parseInt(c.slice(0,2),16),parseInt(c.slice(2,4),16),parseInt(c.slice(4,6),16))}chunk("PLTE",pal);chunk("tRNS",[0]);
 const raw=new Uint8Array((W+1)*H);for(let y=0;y<H;y++)raw.set(data.subarray(y*W,(y+1)*W),y*(W+1)+1);
 const z=[0x78,0x01];let a=1,b=0;for(const v of raw){a=(a+v)%65521;b=(b+a)%65521}for(let i=0;i<raw.length;i+=65535){const n=Math.min(65535,raw.length-i),inv=(~n)&65535;z.push(i+n===raw.length?1:0,n&255,n>>8,inv&255,inv>>8);for(let j=i;j<i+n;j++)z.push(raw[j])}put32(z,((b<<16)|a)>>>0);chunk("IDAT",z);chunk("IEND",[]);
 return Uint8Array.from(arr);
}



import {writeFileSync} from 'node:fs';
const [stage='adult',sex='boy',path='lisyk_'+stage+'_'+sex+'_v1.png']=process.argv.slice(2);
const a=renderAtlas(stage,sex);writeFileSync(path,png(a.W,a.H,a.data,a.colors));
