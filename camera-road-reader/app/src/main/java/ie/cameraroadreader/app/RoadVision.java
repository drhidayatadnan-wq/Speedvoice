package ie.cameraroadreader.app;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;

/**
 * Lightweight, offline, heuristic road marking detection.
 * Not a traffic-sign classifier or lane-keeping system.
 * All detections are tentative and may be incorrect.
 */
final class RoadVision {
  private RoadVision() {}

  static final class SignGuess {
    final String label;
    final Rect rect;
    SignGuess(String label, Rect rect) {
      this.label = label;
      this.rect = rect;
    }
  }

  static final class LaneEstimate {
    final int imageWidth, imageHeight;
    final int yNear, yFar;
    final int leftNear, leftFar, rightNear, rightFar;
    LaneEstimate(int w,int h,int yn,int yf,int ln,int lf,int rn,int rf) {
      imageWidth=w; imageHeight=h;
      yNear=yn; yFar=yf;
      leftNear=ln; leftFar=lf; rightNear=rn; rightFar=rf;
    }
    boolean leftVisible() { return leftNear>=0 && leftFar>=0; }
    boolean rightVisible() { return rightNear>=0 && rightFar>=0; }
    String summary() {
      if(leftVisible() && rightVisible()) return "Possible left + right road markings";
      if(leftVisible()) return "Possible marking on the left";
      if(rightVisible()) return "Possible marking on the right";
      return "Lane markings not detected";
    }
  }

  private static boolean red(int p) {
    int r=Color.red(p), g=Color.green(p), b=Color.blue(p);
    return r>115 && r>g*1.5f && r>b*1.45f;
  }
  private static boolean white(int p) {
    return Color.red(p)>185 && Color.green(p)>185 && Color.blue(p)>185;
  }
  private static boolean nearWhiteOrYellow(int p) {
    int r=Color.red(p),g=Color.green(p),b=Color.blue(p);
    return (r>162 && g>160 && b>152 && Math.abs(r-g)<65)
      || (r>158 && g>135 && b<145 && r>b+45);
  }
  private static boolean inBounds(Bitmap image,int x,int y) {
    return x>=0 && y>=0 && x<image.getWidth() && y<image.getHeight();
  }

  /** Simple red disc with a white horizontal stripe test (Irish RUS 050). */
  static SignGuess possibleNoEntry(Bitmap image) {
    int w=image.getWidth(),h=image.getHeight();
    int minR=Math.max(13,h/28), maxR=Math.max(minR+2,h/11);
    for(int cy=Math.max(maxR,h/12);cy<h*0.70;cy+=Math.max(18,h/25)) {
      for(int cx=Math.max(maxR,w/12);cx<w-w/12;cx+=Math.max(21,w/32)) {
        for(int radius=minR;radius<=maxR;radius+=Math.max(8,h/55)) {
          if(cx-radius<1||cx+radius>=w||cy-radius<1||cy+radius>=h)continue;
          int disc=0;
          for(int i=0;i<16;i++){
            double t=2*Math.PI*i/16;
            int x=(int)(cx+.72*radius*Math.cos(t));
            int y=(int)(cy+.72*radius*Math.sin(t));
            if(red(image.getPixel(x,y)))disc++;
          }
          if(disc<11)continue;
          int stripe=0;
          for(int i=-4;i<=4;i++){
            int x=cx+i*radius/10;
            if(white(image.getPixel(x,cy)))stripe++;
          }
          if(stripe<7)continue;
          int upper=0,lower=0;
          for(int dx=-2;dx<=2;dx++){
            int x=cx+dx*radius/8;
            if(red(image.getPixel(x,cy-radius/3)))upper++;
            if(red(image.getPixel(x,cy+radius/3)))lower++;
          }
          if(upper<3||lower<3)continue;
          return new SignGuess("NO ENTRY TO VEHICLES",new Rect(cx-radius,cy-radius,cx+radius,cy+radius));
        }
      }
    }
    return null;
  }

  /**
   * Detect only the visual family of a possible yellow warning diamond or
   * orange roadworks diamond. This CANNOT identify the black symbol.
   * Coarse colour segmentation followed by a diamond-like region check.
   * False positives are possible; these findings must stay silent.
   */
  static SignGuess possibleIrishWarningFamily(Bitmap image) {
    final int w=image.getWidth(),h=image.getHeight();
    final int step=Math.max(5,Math.round(w/145f));
    final int gw=(w+step-1)/step,gh=(Math.min(h,(int)(h*.78f))+step-1)/step;
    if(gw<12||gh<12)return null;
    byte[] classes=new byte[gw*gh];
    for(int gy=1;gy<gh-1;gy++){
      int y=Math.min(h-1,gy*step);
      for(int gx=1;gx<gw-1;gx++){
        int x=Math.min(w-1,gx*step);
        int p=image.getPixel(x,y);
        int r=Color.red(p),g=Color.green(p),b=Color.blue(p);
        if(r>138&&g>124&&b<130&&r-g<76&&g>b*1.40){
          classes[gy*gw+gx]=1; // RSA yellow warning
        }else if(r>145&&g>60&&g<172&&r>g*1.30&&b<g*.90){
          classes[gy*gw+gx]=2; // orange roadworks
        }
      }
    }
    boolean[] seen=new boolean[classes.length];
    int[] queue=new int[classes.length];
    int bestScore=0;
    SignGuess best=null;
    for(int seed=0;seed<classes.length;seed++){
      int type=classes[seed];
      if(type==0||seen[seed])continue;
      int head=0,tail=0,minx=gw,maxx=0,miny=gh,maxy=0,count=0;
      seen[seed]=true;queue[tail++]=seed;
      while(head<tail){
        int at=queue[head++],x=at%gw,y=at/gw;
        count++;
        if(x<minx)minx=x;if(x>maxx)maxx=x;
        if(y<miny)miny=y;if(y>maxy)maxy=y;
        for(int dy=-1;dy<=1;dy++)for(int dx=-1;dx<=1;dx++){
          if(dx==0&&dy==0)continue;
          int xx=x+dx,yy=y+dy;
          if(xx<0||yy<0||xx>=gw||yy>=gh)continue;
          int next=yy*gw+xx;
          if(!seen[next]&&classes[next]==type){
            seen[next]=true;queue[tail++]=next;
          }
        }
      }
      int bw=maxx-minx+1,bh=maxy-miny+1;
      if(bw<6||bh<6||bw>gw*.34||bh>gh*.52)continue;
      float aspect=bw/(float)bh;
      if(aspect<.63||aspect>1.45)continue;
      float density=count/(float)(bw*bh);
      if(density<.22||density>.83)continue;
      int top=0,mid=0,bot=0;
      int yTop=miny+bh/5,yMid=miny+bh/2,yBot=maxy-bh/5;
      for(int x=minx;x<=maxx;x++){
        if(classes[yTop*gw+x]==type)top++;
        if(classes[yMid*gw+x]==type)mid++;
        if(classes[yBot*gw+x]==type)bot++;
      }
      // Diamond sides narrow toward both top and bottom.
      if(mid<Math.max(3,(int)(top*1.25))||mid<Math.max(3,(int)(bot*1.25)))continue;
      int score=count;
      if(score>bestScore){
        bestScore=score;
        Rect rect=new Rect(minx*step,miny*step,Math.min(w,(maxx+1)*step),
          Math.min(h,(maxy+1)*step));
        best=new SignGuess(type==1?"WARNING DIAMOND":"ROADWORKS DIAMOND",rect);
      }
    }
    return best;
  }

  /** A rough measurement of visible high-contrast white/yellow painted stripes.
   * Only draws lines if both sample rows have plausible, distinct marks.
   */
  static LaneEstimate lanes(Bitmap image) {
    int w=image.getWidth(),h=image.getHeight();
    int far=(int)(h*.63f), near=(int)(h*.89f);
    int lf=searchStripe(image,far,true),ln=searchStripe(image,near,true);
    int rf=searchStripe(image,far,false),rn=searchStripe(image,near,false);
    if(lf<0||ln<0||Math.abs(ln-lf)>w*.23f){lf=-1;ln=-1;}
    if(rf<0||rn<0||Math.abs(rn-rf)>w*.23f){rf=-1;rn=-1;}
    if(ln>=0 && rn>=0 && rn-ln<w*.18f){lf=-1;ln=-1;rf=-1;rn=-1;}
    return new LaneEstimate(w,h,near,far,ln,lf,rn,rf);
  }

  private static int searchStripe(Bitmap b,int y,boolean left){
    int w=b.getWidth();
    int from=(int)(w*(left?.06:.53)),to=(int)(w*(left?.47:.94));
    int best=-1;
    double bestScore=0;
    int step=Math.max(2,w/320);
    for(int x=from;x<to;x+=step) {
      int p=b.getPixel(x,y);
      if(!nearWhiteOrYellow(p))continue;
      int bright=(Color.red(p)+Color.green(p)+Color.blue(p))/3;
      int x1=Math.max(0,x-Math.max(9,w/90));
      int x2=Math.min(w-1,x+Math.max(9,w/90));
      int p1=b.getPixel(x1,y),p2=b.getPixel(x2,y);
      int outer1=(Color.red(p1)+Color.green(p1)+Color.blue(p1))/3;
      int outer2=(Color.red(p2)+Color.green(p2)+Color.blue(p2))/3;
      int contrast=bright-Math.max(outer1,outer2);
      if(contrast<38)continue;
      int vertical=0;
      for(int dy=-10;dy<=10;dy+=5){
        int yy=y+dy;
        if(yy<=0||yy>=b.getHeight())continue;
        for(int dx=-3;dx<=3;dx+=3) {
          if(inBounds(b,x+dx,yy)&&nearWhiteOrYellow(b.getPixel(x+dx,yy))) {
            vertical++;break;
          }
        }
      }
      if(vertical<3)continue;
      double score=contrast*1.0+vertical*8;
      // Bias slightly toward a line closer to the image centre, away from kerbs.
      score-=Math.abs(x-(left?w*.33:w*.67))*.025;
      if(score>bestScore){bestScore=score;best=x;}
    }
    return best;
  }
}
