package ie.speedvoice;
import android.graphics.Bitmap;
import android.graphics.Rect;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import java.util.HashSet;
import java.util.Set;
import java.util.function.IntConsumer;
final class VisualReader {
 private final TextRecognizer recognizer=TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
 void close(){recognizer.close();}
 void scan(Bitmap bmp,IntConsumer result){
 try{recognizer.process(InputImage.fromBitmap(bmp,0)).addOnCompleteListener(task->{
 int limit=-1;
 try{if(task.isSuccessful()){
 Set<Integer> found=new HashSet<>();int matches=0;
 for(Text.TextBlock block:task.getResult().getTextBlocks())for(Text.Line line:block.getLines())for(Text.Element e:line.getElements()){
 String s=e.getText().trim();Rect b=e.getBoundingBox();
 if(b==null||!s.matches("[0-9]{2,3}"))continue;
 int v=Integer.parseInt(s);if(v<10||v>130||v%5!=0)continue;
 int w=bmp.getWidth(),h=bmp.getHeight();
 if(w>=h||b.centerY()<h*.40||b.centerY()>h*.93||(b.centerX()>w*.30&&b.centerX()<w*.70))continue;
 if(SignGeometry.ring(bmp::getPixel,w,h,b.left,b.top,b.right,b.bottom)){found.add(v);matches++;}
 }if(found.size()==1&&matches==1)limit=found.iterator().next();
 }}catch(RuntimeException ignored){}finally{bmp.recycle();result.accept(limit);}
 });}catch(RuntimeException e){bmp.recycle();result.accept(-1);}
 }
}