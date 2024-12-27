/***************************************************************************
                          relay.h  -  description
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


#ifndef SMTPRC_RELAY_H
#define SMTPRC_RELAY_H

/*****************************INCLUDES**************************************/


#include <stdio.h>
#include <stdlib.h>
#include <errno.h>
#include <string.h>
#include <strings.h>
#include <netdb.h>
#include <sys/types.h>
#include <netinet/in.h>
#include <sys/socket.h>
#include <unistd.h>
#include <fcntl.h>
#include <arpa/inet.h>
#include <signal.h>
#include <sys/time.h>
#include <ctype.h>
#include <fcntl.h>
#include <sys/wait.h>
#include <sys/file.h>
#include <netdb.h>
#include <pthread.h>
#include <netdb.h>
#include <time.h>
#include <dirent.h>


/****************************DEFINES****************************************/

#ifndef _REENTRANT
#define _REENTRANT
#endif
#ifndef MSG_NOSIGNAL
#define MSG_NOSIGNAL        0x4000
#endif

#define PORT 25                	//SMTP Port
#define MAXDATA 8192           	//Max data size
#define LARGESTRING 65535				//Large string size
#define CTIMEOUT 10             //Default connect timeout
#define RTIMEOUT 60             //Default read timeout
#define MTIMEOUT 60        			//Default wait for email timeout
#define THREAD_DEFAULT 1000			//default number of threads to scan with
#define CONFIG_FILE "/usr/local/etc/smtprc/rcheck.conf"
#define EMAIL_TEMPLATE "/usr/local/etc/smtprc/email.tmpl"

#define RSET "RSET\r\n"
#define DATA "DATA\r\n"
#define QUIT "QUIT\r\n"

#define SLASHDOT "./"

#define PADDING 60



/****************************DATA STRUCTURES*********************************/

typedef struct _check {    //This structure will hole all of the check info

	unsigned char failed;
	unsigned char passed;
	unsigned char error_code;

	char *helo;              //helo sent to the sever
	char *mail_from;         //mail from sent to the server
	char *rcpt_to;           //rcpt to sent to the server

	char *r_banner;          //banner reply
	char *r_helo;            //helo reply etc etc
	char *r_mail_from;
	char *r_rcpt_to;
	char *r_data_start;
	char *r_data_end;
	char *r_reset;

}
check;

typedef struct _host {     						//this struct will hold all of the host info.

	unsigned char smtp_open;           //this indicates if the server has port 25 open
	unsigned char resolved;            //this indicates wether the ip address was resolved
	unsigned char fatal_error;         //this indicates a fatal error
	unsigned char fatal;

	char *ip_address;
	char *hostname;

	char *r_quit;

	check **smtp_check;

}
host;

typedef struct _rule {

	char *helo;
	char *mail_from;
	char *rcpt_to;

}
rule;






#endif

