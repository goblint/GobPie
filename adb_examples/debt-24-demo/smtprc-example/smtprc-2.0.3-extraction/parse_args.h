/***************************************************************************
                          parse_args.h  -  description
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


#ifndef SMTPRC_PARSE_ARGS_H
#define SMTPRC_PARSE_ARGS_H

#include "options.h"
#include "utils.h"
#include <stdlib.h>
#include <stdio.h>
#include <unistd.h>

/********************************defines************************************/

#ifndef FALSE
#define FALSE 0
#endif

#ifndef TRUE
#define TRUE 1
#endif

/******************************prototypes***********************************/

void parse_args(int argc, char * const *argv);

#endif

