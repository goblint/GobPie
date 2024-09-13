/***************************************************************************
                          parse_config_files.c  -  description
                             -------------------
    begin                : Wed May 21 2003
    copyright            : (C) 2003 by Spencer Hardy
    email                : diceman@dircon.co.uk
 ***************************************************************************/

/***************************************************************************
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *                                                                         *
 ***************************************************************************/

#include "utils.h"
#include "options.h"

extern struct options o;
extern struct flags f;


/*
 * Parse ip address range into individual ip addresses
 *
 * This is really quite messy and could do with a rewrite
 * but it's staying for now.
 *
 */

int get_ip_range(char *iprange) {

	int start[4];
	int end[4];
	int i = 0;

	for(i=0;i<4;i++) {
		start[i] = 1;
		end[i] = 255;
	}

	for(i=0;i<1;i++) {
		for(;start[i]<=end[i];start[i]++) {
			for(;start[i+1]<=end[i+1];start[i+1]++) {
				for(;start[i+2]<=end[i+2];start[i+2]++) {
					for(;start[i+3]<=end[i+3];start[i+3]++) {
						o.no_hostnames++;
					}
				}
			}
		}
	}
				
	return(0);
}