package ie.speedvoice;
import android.graphics.Bitmap;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import java.util.function.IntConsumer;
import java.util.HashSet;
// This reader targets only the speed badge in the supplied simulated portrait MP4.
final class VideoSpeedReader {
 private final TextRecognizer recognizer=TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
 static Bitmap crop(Bitmap image){
  int w=image.getWidth(),h=image.getHeight();
  if(w>=h)return null;
  int x=Math.round(w*.185f), y=Math.round(h*.815f);
  int width=Math.round(w*.13f), height=Math.round(h*.030f);
  Bitmap region=Bitmap.createBitmap(image,x,y,width,height);
  Bitmap enlarged=Bitmap.createScaledBitmap(region,width*4,height*4,true);
  if(region!=image&&region!=enlarged)region.recycle();
  return enlarged;
 }
 void scan(Bitmap bmp,IntConsumer cb){
  try{recognizer.process(InputImage.fromBitmap(bmp,0)).addOnCompleteListener(task->{
   int val=-1;try{if(task.isSuccessful()){
    HashSet<Integer> found=new HashSet<>();
    for(Text.TextBlock b:task.getResult().getTextBlocks())for(Text.Line l:b.getLines())for(Text.Element e:l.getElements()){
     String t=e.getText().trim();if(t.matches("[0-9]{1,3}")){int n=Integer.parseInt(t);if(n<=250)found.add(n);}
    }
    if(found.size()==1)val=found.iterator().next();
   }}catch(RuntimeException ignored){}finally{bmp.recycle();cb.accept(val);}
  });}catch(RuntimeException e){bmp.recycle();cb.accept(-1);}
 }
 void close(){recognizer.close();}
}
