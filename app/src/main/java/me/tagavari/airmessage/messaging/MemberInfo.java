package me.tagavari.airmessage.messaging;

import java.io.Serializable;

public class MemberInfo implements Serializable {
	private static final long serialVersionUID = 0;
	
	private final String name;
	private int color;
	
	public MemberInfo(String name, int color) {
		this.name = name;
		this.color = color;
	}
	
	public String getName() {
		return name;
	}
	
	public int getColor() {
		return color;
	}
	
	public void setColor(int color) {
		this.color = color;
	}
}
