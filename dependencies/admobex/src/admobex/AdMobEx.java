package admobex;
import java.util.Date;
import java.util.Queue;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Window;
import android.view.WindowManager;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.audiofx.AudioEffect.OnControlStatusChangeListener;
import android.widget.RelativeLayout;
import android.view.ViewGroup;
import org.haxe.extension.Extension;
import android.view.Gravity;
import android.view.View;
import android.util.Log;
import android.provider.Settings.Secure;
import java.security.MessageDigest;

import com.google.android.gms.ads.*;

public class AdMobEx extends Extension {

	//////////////////////////////////////////////////////////////////////////////////////////////////
	//////////////////////////////////////////////////////////////////////////////////////////////////

	private InterstitialAd interstitial;
	private AdView banner;
	private RelativeLayout rl;
	private AdRequest adReq;

	//////////////////////////////////////////////////////////////////////////////////////////////////
	//////////////////////////////////////////////////////////////////////////////////////////////////

	private static Boolean failInterstitial=false;
	private static Boolean loadingInterstitial=false;
	private static String interstitialId=null;

	private static Boolean failBanner=false;
	private static Boolean loadingBanner=false;
	private static Boolean mustBeShowingBanner=false;
	private static String bannerId=null;

	private static AdMobEx instance=null;
	private static Boolean testingAds=false;
	private static int gravity=Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;

	//////////////////////////////////////////////////////////////////////////////////////////////////
	//////////////////////////////////////////////////////////////////////////////////////////////////

	public static AdMobEx getInstance(){
		if(instance==null && bannerId!=null) instance = new AdMobEx();
		if(bannerId==null){
			Log.e("AdMobEx","You tried to get Instance without calling INIT first on AdMobEx class!");
		}
		return instance;
	}


	public static void init(String bannerId, String interstitialId, String gravityMode, boolean testingAds){
		AdMobEx.bannerId=bannerId;
		AdMobEx.interstitialId=interstitialId;
		AdMobEx.testingAds=testingAds;
		if(gravityMode.equals("TOP")){
			AdMobEx.gravity=Gravity.TOP | Gravity.CENTER_HORIZONTAL;
		}
		mainActivity.runOnUiThread(new Runnable() {
			public void run() { getInstance(); }
		});	
	}


	public static boolean showInterstitial() {
		Log.d("AdMobEx","Show Interstitial: Begins");
		if(loadingInterstitial) return false;
		if(failInterstitial){
			mainActivity.runOnUiThread(new Runnable() {
				public void run() { getInstance().reloadInterstitial();}
			});	
			Log.d("AdMobEx","Show Interstitial: Interstitial not loaded... reloading.");
			return false;
		}

		if(interstitialId=="") {
			Log.d("AdMobEx","Show Interstitial: InterstitialID is empty... ignoring.");
			return false;
		}
		if(!getInstance().interstitial.isLoaded()) {
			Log.d("AdMobEx","Show Interstitial: Not loaded... ignoring.");
			return false;
		}
		mainActivity.runOnUiThread(new Runnable() {
			public void run() {	getInstance().interstitial.show(); }
		});
		Log.d("AdMobEx","Show Interstitial: Compelte.");
		return true;
	}


	public static void showBanner() {
		if(bannerId=="") return;
		mustBeShowingBanner=true;
		if(failBanner){
			mainActivity.runOnUiThread(new Runnable() {
				public void run() {getInstance().reloadBanner();}
			});
			return;
		}
		Log.d("AdMobEx","Show Banner");
		
		mainActivity.runOnUiThread(new Runnable() {
			public void run() {
				getInstance().rl.removeView(getInstance().banner);
				getInstance().rl.addView(getInstance().banner);
				getInstance().rl.bringToFront();
				getInstance().banner.setVisibility(View.VISIBLE);
			}
		});
	}


	public static void hideBanner() {
		if(bannerId=="") return;
		mustBeShowingBanner=false;
		if(failBanner){
			mainActivity.runOnUiThread(new Runnable() {
				public void run() {getInstance().reloadBanner();}
			});
			return;
		}
		Log.d("AdMobEx","Hide Banner");

		mainActivity.runOnUiThread(new Runnable() {
			public void run() {	getInstance().banner.setVisibility(View.INVISIBLE); }
		});
	}

	public static void onResize(){
		Log.d("AdMobEx","On Resize");
		mainActivity.runOnUiThread(new Runnable() {
			public void run() {	getInstance().reinitBanner(); }
		});
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////
	//////////////////////////////////////////////////////////////////////////////////////////////////

	private AdMobEx() {

		AdRequest.Builder builder = new AdRequest.Builder();

		if(testingAds){
			String android_id = Secure.getString(mainActivity.getContentResolver(), Secure.ANDROID_ID);
    	    String deviceId = md5(android_id).toUpperCase();
			Log.d("AdMobEx","DEVICE ID: "+deviceId);
			builder.addTestDevice(deviceId);
		}
		
		builder.addTestDevice(AdRequest.DEVICE_ID_EMULATOR);
		adReq = builder.build();

		if(bannerId!=""){
			this.reinitBanner();
		}
		
		if(interstitialId!=""){
			interstitial = new InterstitialAd(mainActivity);
			interstitial.setAdUnitId(interstitialId);
			interstitial.setAdListener(new AdListener() {
				public void onAdLoaded() {
					AdMobEx.getInstance().loadingInterstitial=false;
					Log.d("AdMobEx","Received Interstitial!");
				}
				public void onAdFailedToLoad(int errorcode) {
					AdMobEx.getInstance().loadingInterstitial=false;	
					AdMobEx.getInstance().failInterstitial=true;
					Log.d("AdMobEx","Fail to get Interstitial: "+errorcode);
				}
				public void onAdClosed() {
					AdMobEx.getInstance().reloadInterstitial();
					Log.d("AdMobEx","Dismiss Interstitial");
				}
			});
			this.reloadInterstitial();
		}
	}

	private void reinitBanner(){
		if(loadingBanner) return;	
		if(banner==null){ // if this is the first time we call this function
			rl = new RelativeLayout(mainActivity);
			rl.setGravity(gravity);
		} else {
			ViewGroup parent = (ViewGroup) rl.getParent();
			parent.removeView(rl);
			rl.removeView(banner);
			banner.destroy();
		}

		banner = new AdView(mainActivity);
		banner.setAdUnitId(bannerId);
		banner.setAdSize(AdSize.SMART_BANNER);
		banner.setAdListener(new AdListener() {
			public void onAdLoaded() {
				AdMobEx.getInstance().loadingBanner=false;	
				Log.d("AdMobEx","Received Banner OK!");
				if(AdMobEx.getInstance().mustBeShowingBanner){
					AdMobEx.getInstance().showBanner();
				}else{
					AdMobEx.getInstance().hideBanner();
				}				
			}
			public void onAdFailedToLoad(int errorcode) {
				AdMobEx.getInstance().loadingBanner=false;
				AdMobEx.getInstance().failBanner=true;
				Log.d("AdMobEx","Fail to get Banner: "+errorcode);				
			}
		});

		RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
			RelativeLayout.LayoutParams.MATCH_PARENT,
			RelativeLayout.LayoutParams.MATCH_PARENT);					
		mainActivity.addContentView(rl, params);
		rl.addView(banner);
		rl.bringToFront();
		reloadBanner();
	}

	private void reloadInterstitial(){
		if(interstitialId=="") return;
		if(loadingInterstitial) return;
		Log.d("AdMobEx","Reload Interstitial");
		loadingInterstitial=true;
		interstitial.loadAd(adReq);
		failInterstitial=false;
	}

	private void reloadBanner(){
		if(bannerId=="") return;
		if(loadingBanner) return;
		Log.d("AdMobEx","Reload Banner");
		loadingBanner=true;
		banner.loadAd(adReq);
		failBanner=false;
	}

	private static String md5(String s)  {
		MessageDigest digest;
		try  {
		    digest = MessageDigest.getInstance("MD5");
		    digest.update(s.getBytes(),0,s.length());
		    return new java.math.BigInteger(1, digest.digest()).toString(16);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "";
	}

}
