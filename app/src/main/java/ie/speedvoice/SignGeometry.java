package ie.speedvoice;
final class SignGeometry {
 interface Pixels{int get(int x,int y);}
 static boolean red(int p){int r=(p>>16)&255,g=(p>>8)&255,b=p&255;return r>=120&&r-g>=48&&r-b>=40&&r>=1.45*g&&r>=1.35*b;}
 static boolean pale(int p){int r=(p>>16)&255,g=(p>>8)&255,b=p&255;return Math.min(r,Math.min(g,b))>=155&&Math.max(r,Math.max(g,b))-Math.min(r,Math.min(g,b))<=50;}
 static boolean ring(Pixels px,int w,int h,int left,int top,int right,int bottom){
 double tw=right-left,th=bottom-top;
 if(tw<9||th<9||tw>0.15*w||th>0.12*h||tw/th>3.2||tw/th<0.4)return false;
 double cx=(left+right)/2.0,cy=(top+bottom)/2.0,base=Math.max(tw/2,th/2);
 for(double k=1.25;k<=2.6;k+=0.12){double rad=base*k;
 for(double dx:new double[]{-0.12,0,0.12})for(double dy:new double[]{-0.12,0,0.12}){
 double x=cx+dx*rad,y=cy+dy*rad;
 if(x-rad*1.15<0||y-rad*1.15<0||x+rad*1.15>=w||y+rad*1.15>=h)continue;
 int hits=0;int[] quadrants=new int[4];int whites=0;
 for(int i=0;i<32;i++){double a=i*Math.PI/16;boolean hit=false;
 for(double s:new double[]{0.9,0.97,1.04,1.11})if(red(px.get((int)Math.round(x+rad*s*Math.cos(a)),(int)Math.round(y+rad*s*Math.sin(a))))){hit=true;break;}
 if(hit){hits++;quadrants[i/8]++;}
 if(pale(px.get((int)Math.round(x+rad*0.69*Math.cos(a)),(int)Math.round(y+rad*0.69*Math.sin(a)))))whites++;}
 if(hits>=29&&whites>=21&&quadrants[0]>=6&&quadrants[1]>=6&&quadrants[2]>=6&&quadrants[3]>=6)return true;
 }}return false;
 }
}