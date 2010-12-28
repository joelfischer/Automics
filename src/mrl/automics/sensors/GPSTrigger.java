package mrl.automics.sensors;

public class GPSTrigger {
	
	public double lat;
	public double lng;
	public String type;
	public String comment;
	public int id;
	public int radius;
	public boolean fired;
	public boolean keepChecking; //set this to true if it is a trigger that needs to be continued to check, e.g. until a certain time
								//has elapsed, e.g. not to fire immediately, e.g. for break zones
	
	
	public GPSTrigger(int id, String type, int radius, String comment, double lat, double lng) {
		this.id = id;
		this.type = type;
		this.radius = radius;
		this.comment = comment;
		this.lat = lat;
		this.lng = lng;
	}
	
	public void setLat(double lat) {
		this.lat = lat;
	}
	
	public void setLng(double lng) {
		this.lng = lng;
	}
	
	public void setId(int id) {
		this.id = id;
	}
	
	public void setComment(String cmnt) {
		this.comment = cmnt;
	}
	
	public void setType (String type) {
		this.type = type;
	}
	
	public void hasFired (boolean fired) {
		this.fired = fired;
	}
	
	
	

}
