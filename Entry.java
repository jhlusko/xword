package dictionary;

import java.awt.Point;

public class Entry {
	
	Clue wordGroup;
	Point point; 
	boolean isHorizontal;
	int offset;
	Point origin;

	public Entry(Clue w, Point p, int o, boolean isHorizontal){
		this.wordGroup = w;
		this.point = p;
		this.offset = o;
		this.isHorizontal = isHorizontal;
		this.origin = getOrigin(p, o, isHorizontal);
		
	}

	private Point getOrigin(Point point, int offset, boolean isHorizontal) {
		Point origin = new Point(point.x - offset*((isHorizontal)?1:0), point.y - offset*((isHorizontal)?0:1));
		return origin;
		
	}
	
	

}
