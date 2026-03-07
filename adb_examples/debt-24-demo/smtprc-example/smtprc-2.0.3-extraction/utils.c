/***************************************************************************
                          utils.c  -  description
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
#include "relay.h"
#include "options.h"

extern struct options o;
extern struct flags f;

extern host **hosts;


/*
 * Safe malloc routine. checks for errors etc..
 *
 */
 
void *s_malloc(unsigned long size) {

  void *mymem;
  mymem=malloc(size);
  return(mymem);
  
}

/*
 * prints debugging information only if the
 * debugging flag is set
 *
 */
 
void debug(char *fmt, ...) {

  if(f.debug) {
		va_list ap;
  	va_start(ap, fmt);
 		fflush(stdout);
		fprintf(stdout, "DEBUG: ");
  	vfprintf(stderr, fmt, ap);
  	fprintf(stderr, "\n");
  	va_end(ap);
	}

  return;

}
