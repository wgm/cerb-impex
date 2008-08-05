package com.cerb4.impex;

import com.cerb4.impex.exporters.Ticket;
import com.cerb4.impex.exporters.Worker;


public class Main {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
//		new Address().export();
		new Ticket().export();
		new Worker().export();
	}

}
