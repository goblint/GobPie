/***************************************************************************
						  smtprc.c  -  description
							 -------------------
	begin                : Wed May 21 08:13:08 BST 2003
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
#include "parse_args.h"
#include "parse_config_files.h"
#include "scan_engine.h"

extern struct options o;
extern struct flags f;

int main(int argc, char *argv[]) {

	parse_args(argc, argv);             //parses the command line args
	get_ip_range(o.ip_range);           //parse the ip range and save into memory
	/* ..... */

	printf("Starting the scan....... Please wait!\n\n\n");

	start_scan();

	/* ..... */

	return EXIT_SUCCESS;
}
