package ie.nightpdf.reader;
import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.net.Uri;
import android.webkit.*;
import android.graphics.Color;
import androidx.webkit.WebViewAssetLoader;
public class MainActivity extends Activity {
 private static final int PICK=91;
 private WebView web;
 private ValueCallback<Uri[]> callback;
 @Override public void onCreate(Bundle state) {
  super.onCreate(state);getWindow().setStatusBarColor(Color.BLACK);getWindow().setNavigationBarColor(Color.BLACK);
  web=new WebView(this);web.setBackgroundColor(Color.BLACK);setContentView(web);
  WebSettings s=web.getSettings();s.setJavaScriptEnabled(true);s.setDomStorageEnabled(true);s.setAllowFileAccess(false);s.setAllowContentAccess(true);s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
  WebViewAssetLoader loader=new WebViewAssetLoader.Builder().addPathHandler("/assets/",new WebViewAssetLoader.AssetsPathHandler(this)).build();
  web.setWebViewClient(new WebViewClient(){
   @Override public WebResourceResponse shouldInterceptRequest(WebView view,WebResourceRequest req){
    Uri u=req.getUrl();if("appassets.androidplatform.net".equals(u.getHost())&&"https".equals(u.getScheme())){WebResourceResponse r=loader.shouldInterceptRequest(u);if(r!=null&&u.getPath()!=null&&u.getPath().endsWith(".mjs"))r.setMimeType("application/javascript");return r;}
    return new WebResourceResponse("text/plain","UTF-8",new java.io.ByteArrayInputStream(new byte[0]));
   }
   @Override public boolean shouldOverrideUrlLoading(WebView v,WebResourceRequest r){return !"appassets.androidplatform.net".equals(r.getUrl().getHost());}
  });
  web.setWebChromeClient(new WebChromeClient(){
   @Override public boolean onShowFileChooser(WebView view,ValueCallback<Uri[]> cb,FileChooserParams params){
    if(callback!=null)callback.onReceiveValue(null);callback=cb;
    Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("application/pdf");
    try{startActivityForResult(i,PICK);}catch(Exception e){callback.onReceiveValue(null);callback=null;}return true;
   }
  });
  web.loadUrl("https://appassets.androidplatform.net/assets/index.html");
 }
 @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(request==PICK&&callback!=null){callback.onReceiveValue(result==RESULT_OK&&data!=null&&data.getData()!=null?new Uri[]{data.getData()}:null);callback=null;}}
 @Override protected void onDestroy(){if(callback!=null){callback.onReceiveValue(null);callback=null;}if(web!=null)web.destroy();super.onDestroy();}
}