/***************************************************************************
                          parse_args.c  -  description
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

#include "parse_args.h"
#include "utils.h"

struct options o;
struct flags f;


void parse_args(int argc, char * const *argv) {


	char c;

	while((c = getopt(argc, argv, "ab:c:de:f:g:hi:j:k:l:m:no:p:qr:s:tu:vw:x:y:"))!=-1) {


		switch(c) {
			case 'a':		//display all results even those that pass
				f.display_all = TRUE;
				break;
			case 'b':  //email addy to use to replace --NAME-- --DOMAIN--
				f.send_email = TRUE;
				o.email_address=s_malloc((strlen(optarg)+1) * sizeof(char));
				strncpy(o.email_address, optarg, strlen(optarg));
				o.email_address[strlen(optarg)] = '\0';
				break;
			case 'c':  //Specify the config file to use
				f.config_file = TRUE;
				//o.config_file=s_malloc((strlen(optarg)+1) * sizeof(char));
				strncpy(o.config_file, optarg, strlen(optarg));
				o.config_file[strlen(optarg)] = '\0';
				break;
			case 'd':  //Switch on DEBUG MODE
				f.debug = TRUE;
				break;
			case 'e':  //Specify a local mailbox
				f.check_mailbox = TRUE;
				f.mbox = TRUE;
				o.mailbox=s_malloc((strlen(optarg)+1) * sizeof(char));
				strncpy(o.mailbox, optarg, strlen(optarg));
				o.mailbox[strlen(optarg)] = '\0';
				break;
			case 'i':	//specify an ip list
				f.ip_list = TRUE;
				o.ip_list=s_malloc((strlen(optarg)+1) * sizeof(char));
				strncpy(o.ip_list, optarg, strlen(optarg));
				o.ip_list[strlen(optarg)] = '\0';
				break;
			case 'j': //specify an auto config file
				f.auto_config = TRUE;
				o.auto_config_file=s_malloc((strlen(optarg)+1) * sizeof(char));
				strncpy(o.auto_config_file, optarg, strlen(optarg));
				o.auto_config_file[strlen(optarg)] = '\0';
				break;
			case 'k': //generate a config file
				f.generate_config = TRUE;
				o.generate_file=s_malloc((strlen(optarg)+1) * sizeof(char));
				strncpy(o.generate_file, optarg, strlen(optarg));
				o.generate_file[strlen(optarg)] = '\0';
				break;
			case 'm': //TIME OUT before checking mail file
				o.m_timeout = atoi(optarg);
				break;
			case 'n': //Resolve hostnames
				f.resolve_hostnames = TRUE;
				break;
			case 'o': //PRINT OUTPUT IN MACHINE READABLE FORMAT
				f.output_machine = TRUE;
				o.machine_file=s_malloc((strlen(optarg)+1) * sizeof(char));
				strncpy(o.machine_file, optarg, strlen(optarg));
				o.machine_file[strlen(optarg)] = '\0';
				break;
			case 'p': //NUMBER OF THREADS
				o.number_of_threads = atoi(optarg);
				break;
			case 'q':
				f.display_only_ips = TRUE;
				break;
			case 's': //IP RANGE TO SCAN
				f.ip_range = TRUE;
				o.ip_range=s_malloc((strlen(optarg)+1) * sizeof(char));
				strncpy(o.ip_range, optarg, strlen(optarg));
				o.ip_range[strlen(optarg)] = '\0';
				break;
			case 'u':  //maildir mailbox
				f.check_mailbox = TRUE;
				f.maildir = TRUE;
				o.mailbox=s_malloc((strlen(optarg)+1) * sizeof(char));
				strncpy(o.mailbox, optarg, strlen(optarg));
				o.mailbox[strlen(optarg)] = '\0';
				break;
			case 'y':  //specify email file
				f.email_template = TRUE;
				//o.email_template=s_malloc((strlen(optarg)+1) * sizeof(char));
				strncpy(o.email_template, optarg, strlen(optarg));
				o.email_template[strlen(optarg)] = '\0';;
				break;
			case 'z':  //output to html file
				f.output_machine = TRUE;
				o.machine_file=s_malloc((strlen(optarg)+1) * sizeof(char));
				strncpy(o.machine_file, optarg, strlen(optarg));
				o.machine_file[strlen(optarg)] = '\0';
			default:
				return;
		}
	}

	return;

}
