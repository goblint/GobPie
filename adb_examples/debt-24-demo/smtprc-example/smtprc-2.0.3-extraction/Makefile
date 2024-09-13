CC=gcc
CFLAGS=-Wall -g 
INSTALL_PROG=install -c
MKDIR=mkdir -p
REMOVE=rm -rf
LIBS=-lsocket -lnsl -lrt -lpthread


all: freebsd
install: freebsd_install
uninstall: freebsd_uninstall



linux: freebsd_build
linux_install: freebsd_install_start
linux_uninstall: freebsd_uninstall_start

freebsd: freebsd_build
freebsd_install: freebsd_install_start
freebsd_uninstall: freebsd_uninstall_start

solaris: solaris_err
solaris_install: solaris_err
solaris_uninstall: solaris_err

solaris_err: 
	@echo " "
	@echo " "
	@echo " "
	@echo "	Unfortunatly SmtpRC does not yet work correctly"
	@echo "	under Solaris."
	@echo " "
	@echo " "
	@echo "	To use SmtpRC you must run it on a *BSD or Linux"
	@echo "	based OS"
	@echo " "
	@echo " "
	@echo " "
	@echo " "
	@echo " "
	@echo " "

freebsd_build: smtprc.o scan_engine.o options.o utils.o parse_config_files.o parse_args.o
	${CC} $(CFLAGS) -c smtprc.c scan_engine.c options.c utils.c parse_config_files.c parse_args.c -pthread
	${CC} -o smtprc smtprc.o scan_engine.o options.o utils.o parse_config_files.o parse_args.o -pthread

freebsd_install_start:
	${INSTALL_PROG} smtprc /usr/local/bin/smtprc
	${INSTALL_PROG} gsmtprc /usr/local/bin/gsmtprc
	-${MKDIR} /usr/local/etc/smtprc
	${INSTALL_PROG} auto.conf /usr/local/etc/smtprc/auto.conf
	${INSTALL_PROG} email.tmpl /usr/local/etc/smtprc/email.tmpl
	${INSTALL_PROG} rcheck.conf /usr/local/etc/smtprc/rcheck.conf
	-${MKDIR} /usr/local/share/doc/smtprc
	${INSTALL_PROG} README /usr/local/share/doc/smtprc/README
	${INSTALL_PROG} FAQ /usr/local/share/doc/smtprc/FAQ
	${INSTALL_PROG} smtprc.1 /usr/local/man/man1/smtprc.1
	${INSTALL_PROG} gsmtprc.1 /usr/local/man/man1/gsmtprc.1

freebsd_uninstall_start:
	-${REMOVE} /usr/local/etc/smtprc
	-${REMOVE} /usr/local/share/doc/smtprc
	-${REMOVE} /usr/local/bin/smtprc
	-${REMOVE} /usr/local/man/man1/smtprc.1
	-${REMOVE} /usr/local/man/man1/gsmtprc.1
	-${REMOVE} /usr/local/bin/gsmtprc

solaris_build: smtprc.o scan_engine.o options.o utils.o parse_config_files.o parse_args.o
				${CC} $(CFLAGS) -c smtprc.c scan_engine.c options.c utils.c parse_config_files.c parse_args.c
				${CC} -o smtprc smtprc.o scan_engine.o options.o utils.o parse_config_files.o parse_args.o ${LIBS}

solaris_install_start:
	${INSTALL_PROG} /usr/local/bin smtprc
	-${MKDIR} /usr/local/etc/smtprc
	${INSTALL_PROG} /usr/local/etc/smtprc auto.conf
	${INSTALL_PROG} /usr/local/etc/smtprc email.tmpl
	${INSTALL_PROG} /usr/local/etc/smtprc rcheck.conf
	-${MKDIR} /usr/local/doc/smtprc
	${INSTALL_PROG} /usr/local/doc/smtprc README
	${INSTALL_PROG} /usr/local/doc/smtprc FAQ
	${INSTALL_PROG} /usr/share/man/man1 smtprc.1
	${INSTALL_PROG} /usr/share/man/man1 gsmtprc.1
	${INSTALL_PROG} /usr/local/bin gsmtprc

solaris_uninstall_start:
	-${REMOVE} /usr/local/etc/smtprc
	-${REMOVE} /usr/local/share/doc/smtprc
	-${REMOVE} /usr/local/bin/smtprc
	-${REMOVE} /usr/local/man/man1/smtprc.1
	-${REMOVE} /usr/local/man/man1/gsmtprc.1
	-${REMOVE} /usr/local/bin/gsmtprc


clean:
	-${REMOVE} *.o smtprc

message:
	@echo " "
	@echo " "
	@echo " "
	@echo "	You need to specify what OS to make...." 
	@echo " "
	@echo "	The three types currently supported are:"  
	@echo " "
	@echo " "
	@echo "	freebsd"
	@echo "	linux"
	@echo " solaris"
	@echo " "
	@echo " "
	@echo " "
	@echo "	Type make <os> to bulid the package and then"
	@echo "	make <os>_install to install the package"
	@echo "	(Swapping <os> for one of the supported types"
	@echo " "
	@echo " "
	@echo "	Example: "
	@echo " "
	@echo "	make freebsd"
	@echo "	make freebsd_install"
	@echo "	make clean"
	@echo " "
	@echo " "
	@echo "	Also:"
	@echo "	make freebsd_unistall"
	@echo " "
	@echo " "
	@echo " "
	

