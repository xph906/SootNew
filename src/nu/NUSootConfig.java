package nu;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import soot.SootMethod;

public class NUSootConfig {
	static NUSootConfig instance = null;
	static public NUSootConfig getInstance(){
		if(instance==null)
			instance = new NUSootConfig();
		return instance;
	}
	
	static public boolean isInternalMethod(SootMethod sm){
		if(sm.getDeclaringClass().getName().startsWith("com.google.android.gms.internal"))
			return true;
		//TODO: needs further supplement.
		return false;
	}
	
	/***Field***/
	//Indicate whether adding event (e.g., onClick) InvokeExpr Stmt 
	//after each listener is initialized.
	private boolean graphEnhanceEnabled; 
	//Disable data-flow analysis on constant propagation by default.
	private boolean fastConstantPropogationAnalysis; 
	//Disable inter-component flow analysis by default.
	private boolean interComponentAnalysisEnabled;
	private List<String> uiEventCallbackList; //UI event callback function list
	private String fullAPKFilePath;           //target APK's location
	private String androidJarPath;            //android platforms' path
	private String apkToolPath;               //apktool location
	private String decompiledAPKOutputPath;   //output path for decompiled apk
	
	
	private NUSootConfig(){
		graphEnhanceEnabled = false;
		fastConstantPropogationAnalysis = true;
		initializeUIEventCallbackList();
	}
	

	/***Setter***/
	public void setGraphEnhanceEnabled(boolean enableGraphEnhance) {
		this.graphEnhanceEnabled = enableGraphEnhance;
	}
	public void setFullAPKFilePath(String fullAPKFilePath) {
		if(!isValidFilePath(fullAPKFilePath)){
			NUDisplay.error(fullAPKFilePath+" is not valid apk path.", 
					"setFullAPKFilePath");
			System.exit(1);
		}
		this.fullAPKFilePath = fullAPKFilePath;
	}
	public void setAndroidJarPath(String androidJarPath) {
		if(!isValidDirectoryPath(androidJarPath)){
			NUDisplay.error(androidJarPath+" is not valid androidJar path.", 
					"setAndroidJarPath");
			System.exit(1);
		}
		this.androidJarPath = androidJarPath;
	}
	public void setApkToolPath(String apkToolPath) {
		if(!isValidFilePath(apkToolPath)){
			NUDisplay.error(apkToolPath+" is not valid apktool path.", 
					"setApkToolPath");
			System.exit(1);
		}
		this.apkToolPath = apkToolPath;
	}
	public void setDecompiledAPKOutputPath(String decompiledAPKOutputPath) {
		if(!isValidDirectoryPath(decompiledAPKOutputPath)){
			NUDisplay.error(decompiledAPKOutputPath+" is not valid decompiled apk path.", 
					"setDecompiledAPKOutputPath");
			System.exit(1);
		}
		this.decompiledAPKOutputPath = decompiledAPKOutputPath;
	}
	public void setFastConstantPropogationAnalysis(
			boolean fastConstantPropogationAnalysis) {
		this.fastConstantPropogationAnalysis = fastConstantPropogationAnalysis;
	}
	public void setInterComponentAnalysisEnabled(
			boolean interComponentAnalysisEnabled) {
		this.interComponentAnalysisEnabled = interComponentAnalysisEnabled;
	}
	
	/***Getter***/
	public boolean isGraphEnhanceEnhanced() {
		return graphEnhanceEnabled;
	}
	public List<String> getUIEventCalllbackList(){
		return uiEventCallbackList;
	}
	public String getFullAPKFilePath() {
		return fullAPKFilePath;
	}
	public String getAndroidJarPath() {
		return androidJarPath;
	}
	public String getApkToolPath() {
		return apkToolPath;
	}
	public String getDecompiledAPKOutputPath() {
		return decompiledAPKOutputPath;
	}
	public boolean isFastConstantPropogationAnalysis() {
		return fastConstantPropogationAnalysis;
	}
	public boolean isInterComponentAnalysisEnabled() {
		return interComponentAnalysisEnabled;
	}
	
	/***Internal Methods***/
	private void initializeUIEventCallbackList(){
		uiEventCallbackList = new ArrayList<String>();
		uiEventCallbackList.add("onClick");
		uiEventCallbackList.add("onLongClick");
		uiEventCallbackList.add("onFocusChange"); //View v, boolean hasFocus
		uiEventCallbackList.add("onFocusChanged"); //View v, boolean hasFocus
		uiEventCallbackList.add("onKey"); //View v, int keyCode, KeyEvent event
		uiEventCallbackList.add("onKeyDown"); //int keyCode, KeyEvent event
		uiEventCallbackList.add("onKeyUp");  //int keyCode, KeyEvent event
		uiEventCallbackList.add("onKeyLongPress"); //int keyCode, KeyEvent event
		uiEventCallbackList.add("onTouchEvent");  //MotionEvent event
		uiEventCallbackList.add("onTouch"); //View v, MotionEvent event
	}
	private boolean isValidFilePath(String path){
		try{
			File f = new File(path);
			if(!f.exists() || !f.isFile())
				return false;
		}
		catch(Exception e){
			NUDisplay.error("failed to verify file path:"+path+" because "+e, "isValidFilePath");
			return false;
		}
		return true;
	}
	private boolean isValidDirectoryPath(String path){
		try{
			File f = new File(path);
			if(!f.exists() || !f.isDirectory())
				return false;
		}
		catch(Exception e){
			NUDisplay.error("failed to verify file path:"+path+" because "+e, "isValidDirectoryPath");
			return false;
		}
		return true;
	}
	
}
