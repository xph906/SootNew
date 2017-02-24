package nu;

public class NUSootConfig {
	static NUSootConfig instance = null;
	static public NUSootConfig getInstance(){
		if(instance==null)
			instance = new NUSootConfig();
		return instance;
	}
	
	private NUSootConfig(){
		enableGraphEnhance = false;
	}

	private boolean enableGraphEnhance;
	
	public boolean isEnableGraphEnhance() {
		return enableGraphEnhance;
	}

	public void setEnableGraphEnhance(boolean enableGraphEnhance) {
		this.enableGraphEnhance = enableGraphEnhance;
	}
}
