/***************************************************************************
                          options.h  -  description
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

#ifndef SMTPRC_OPTIONS_H
#define SMTPRC_OPTIONS_H

/****************************includes**************************************/

#include <stdlib.h>
#include <stdio.h>
#include <unistd.h>
#include <pthread.h>


/*****************************structures***********************************/

struct options {

	//config options
	unsigned int time;                      //timestamp
	unsigned short int number_of_threads;   //max number of threads
	unsigned short int c_timeout;           //connect timeout
	unsigned short int r_timeout;           //read timeout
	unsigned short int m_timeout;           //wait for mail timeout
	unsigned short int cur_threads;         //current number of threads
	unsigned short int no_rules;            //number of rules to scan with;
	unsigned long no_hostnames;             //number of hosts to scan
	unsigned long cur_host;                 //current host being scanned

	pthread_t *tid;

	char *email_address;                    //email_addres to relay to
	char *ip_range;                         //ip address range
	char *ip_list;                          //ip list file
	char *mailbox;                          //mailbox file
	char *auto_config_file;                 //auto config filename
	char *generate_file;                    //filename to generate auto config file
	char *config_file;                      //relay check config file
	char *email_template;                   //email template to be sent through servers being checked
	char *name;                             //name part of the email address specified with -b
	char *domain;                           //domain part of the email address specified with -b

	//output file formats
	char *html_file;                        //output html filename
	char *html_path;                        //path to output html file
	char *xml_file;                         //output xml filename
	char *xml_path;                         //path to xml file
	char *machine_file;                     //output machine readable filename
	char *machine_path;                     //path to machine file
	char *text_file;                        //output text filename
	char *text_path;                        //path to text file

	//email template                        //email body to send through servers
	char *email;                            //email subject to send through servers
	char *email_subject;

	//total time taken
	unsigned char hours;
	unsigned char mins;
	unsigned char seconds;

};


//If a flag is set to TRUE then that option
//has been selected.
struct flags {

	unsigned char debug;                              //debugging mode
	unsigned char verbose;                            //be verbose
	unsigned char check_mailbox;                      //check mailbox after scanning
	unsigned char maildir;                      			//mailbox format is maildir
	unsigned char mbox;		                      			//mailbox format is maildir
	unsigned char pop;		                      			//mailbox format is maildir
	unsigned char auto_config;                        //auto config file specified
	unsigned char generate_config;                    //generate a suto config file
	unsigned char output_html;                        //output to html
	unsigned char output_text;                        //output to text file
	unsigned char output_machine;                     //output to machine readable format
	unsigned char output_xml;                         //output to xml
	unsigned char resolve_hostnames;                  //try to resolve ips to hostnames
	unsigned char display_only_ips;                   //only output ips not hostnames
	unsigned char ip_range;                           //ip list file specified.
	unsigned char ip_list;                            //got ip list
	unsigned char send_email;                         //send email through servers
	unsigned char config_file;                        //got config file
	unsigned char email_template;                     //got email template
	unsigned char got_name_macro;                     //--NAME-- macro specified in the config file
	unsigned char got_email;                          //got email address specified with -b
	unsigned char threads;                            //threads have been specified
	unsigned char display_all;                        //display all results even those that fail

};


/*****************************prototypes************************************/

#endif

