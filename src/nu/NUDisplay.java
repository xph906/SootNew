package nu;

public class NUDisplay {
	static final String DEBUGTAG = "[NULIST DEBUG]";
	static final String TODOTAG = "[NULIST TODO]";
	static final String INFOTAG = "[NULIST OUTPUT]";
	
	static public void debug(String msg, String loc){
		if(loc==null)
			System.out.println(DEBUGTAG+": "+msg);
		else 
			System.out.println(DEBUGTAG+": "+msg+" @"+loc);
	}
	
	static public void info(String msg, String loc){
		if(loc==null)
			System.out.println(INFOTAG+": "+msg);
		else 
			System.out.println(INFOTAG+": "+msg+" @"+loc);
	}
	
	static public void todo(String msg, String loc){
		if(loc==null)
			System.out.println(TODOTAG+": "+msg);
		else 
			System.out.println(TODOTAG+": "+msg+" @"+loc);
	}
}
