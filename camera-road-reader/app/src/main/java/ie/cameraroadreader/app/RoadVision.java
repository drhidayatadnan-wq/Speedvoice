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
          return new SignGuess("NO ENTRY",new Rect(cx-radius,cy-radius,cx+radius,cy+radius));
        }
      }
    }
    return null;
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
