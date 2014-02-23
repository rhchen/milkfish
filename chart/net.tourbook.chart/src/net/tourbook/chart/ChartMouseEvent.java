/*******************************************************************************
 * Copyright (C) 2005, 2011  Wolfgang Schramm and Contributors
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation version 2 of the License.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110, USA
 *******************************************************************************/
package net.tourbook.chart;

public class ChartMouseEvent {

	int				type;

	/**
	 * When <code>true</code> the event is done by the receiver, when <code>false</code> the
	 * receiver has done nothing.
	 */
	public boolean	isWorked	= false;

	public int		devYMouse;
	public int		devXMouse;

	public ChartMouseEvent(final int eventType) {
		type = eventType;
	}
}
