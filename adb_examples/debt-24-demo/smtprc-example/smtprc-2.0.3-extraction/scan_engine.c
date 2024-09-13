/***************************************************************************
                          scan_engine.c  -  description
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

/* Some code is omitted for a faster analysis to illustrate the example */

#include "scan_engine.h"
#include "options.h"
#include "utils.h"

extern struct options o;
extern struct flags f;

pthread_mutex_t main_thread_count_mutex; //mutex for the main thread counter

void start_scan(void) {

	struct timespec tv;
	pthread_t c_tid;
	pthread_attr_t attr;
	unsigned char flag = 0;
	unsigned char res = 0;
	int x = 0;
	size_t stack_size;
	tv.tv_sec = 0;
	tv.tv_nsec = 500000000;


	/* use if we want to start in a detached state but im not going to do this for now
	 * im going to try and join the threads instead
	 *
	 * pthread_attr_init(&attr);
	 * pthread_attr_setdetachstate(&attr, PTHREAD_CREATE_DETACHED);
	 *
	 */

	pthread_mutex_init(&main_thread_count_mutex, NULL);

	debug("\n\nstack size: %d\n", stack_size);

	stack_size = (PTHREAD_STACK_MIN + 0x3000);

	debug("\n\nstack size: %d\n", stack_size);

	o.tid=s_malloc((o.no_hostnames + 1) * sizeof(pthread_t));

	for(x=0;x<o.no_hostnames;x++) {
		o.tid[x] = (pthread_t)0;
	}

	pthread_create(&c_tid, &attr, (void *)cleaner_start, NULL);

	for(o.cur_host=0;o.cur_host<o.no_hostnames;o.cur_host++) {
		pthread_mutex_lock(&main_thread_count_mutex);
		while(o.cur_threads>=o.number_of_threads) {
			pthread_mutex_unlock(&main_thread_count_mutex);
			debug("Sleeping cur child == %d, max child == %d\n", o.cur_threads, o.number_of_threads);
			nanosleep(&tv, NULL);
		}
		pthread_mutex_unlock(&main_thread_count_mutex);
		while((res = pthread_create(&c_tid, &attr, (void *)thread_start, (int *)o.cur_host)) != 0) {
			usleep(200000);
		}
		//printf("c_tid: %d\n", c_tid);
		debug("Created thread\n");
		pthread_mutex_lock(&main_thread_count_mutex);
		o.cur_threads++;
		pthread_mutex_unlock(&main_thread_count_mutex);
	}
	flag = 0;
	pthread_mutex_lock(&main_thread_count_mutex);
	while(o.cur_threads>0) {
		pthread_mutex_unlock(&main_thread_count_mutex);
		flag++;
		if(f.debug||f.verbose>1) {
			if(flag>1) {
				fprintf(stderr, "O.cur_childs(%d) id greater than zero...... sleepingz\n",o.cur_threads);
				flag = 0;
			}
		}
		nanosleep(&tv, NULL);
	}
	pthread_mutex_unlock(&main_thread_count_mutex);
	//pthread_attr_destroy(&attr);

	return;

}


int cleaner_start(void) {

	/*
	 * This is the cleaner thread it scans the
	 * thread exit global variable for any threads
	 * that have exited and then joins them clearing
	 * up any resources they've used.
	 *
	 */

	int x = 0;


	while(1) {
		for(x=0;x<o.no_hostnames;x++) {
			if((int)o.tid[x]) {
				if(!pthread_join(o.tid[x], NULL)) {
					pthread_mutex_lock(&main_thread_count_mutex);
					o.cur_threads--;	
					pthread_mutex_unlock(&main_thread_count_mutex);
					//printf("REAPER KILLED: %d\n", (int)o.tid[x]);
					o.tid[x] = (pthread_t)0;
				}
			}
		}
		usleep(200000);
	}

	return(0);
}

int thread_start(long cur_host) {
	return 0;
}