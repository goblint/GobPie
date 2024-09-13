/***************************************************************************
                          scan_engine.h  -  description
                             -------------------
    begin                : Mon May 26 2003
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

#ifndef SMTPRC_SCAN_ENGINE_H
#define SMTPRC_SCAN_ENGINE_H


#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>


void start_scan(void);
int thread_start(long cur_host);
int cleaner_start(void);

#endif

