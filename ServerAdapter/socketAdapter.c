#include <stdio.h>
#include <string.h>

#ifdef __gnu_linux__
	#include <sys/types.h>
	#include <sys/socket.h>
	#include <netinet/in.h>
	#include <unistd.h>
	#include <stdlib.h>
	#include <netdb.h>
	#include <arpa/inet.h>
	#include <pthread.h>
#elif _WIN32
	#include <winsock.h>
	#include <windows.h>
	#pragma comment(lib, "wsock32.lib")
#else
	#error OS not supported, have fun with adding ifdefs
#endif

int learner_listener_sd, learner_conn_sd, main_sd, secondary_sd, main_sd_old, secondary_sd_old;
#ifdef _WIN32
HANDLE socket_thread;
#elif __gnu_linux__
pthread_t socket_thread;
#endif
int main_socket_blocked;

int server_port, learner_port;
char server_addr[100];

#define default_learner_port 5000
#define default_server_port 34567
#define default_server_addr "1.2.3.4"

#define input_buffer_size 1025
#define output_buffer_size 1025
char output_buffer[output_buffer_size];

#define syn_ok 0

struct sockaddr_in serv_addr_struct;
struct sockaddr_in local_addr;

void error(char* msg) {
	printf("error: %s\naborting\n", msg);
	exit(-1);
}

void strcpy_end(char* dest, char* src, int maxsize) {
	int len = strlen(src)+1;
	if (maxsize < len) {
		len = maxsize;
	}
	if (len == 0) {
		error("cannot copy 0 characters safely!"); // not very subtle, but I want to know these things
	}
	strncpy(dest, src, len-1);
	dest[len-1] = '\0';
}

void answer(char* output) {
	send(learner_conn_sd, output, strlen(output), 0);
}

//convert newline-terminated input to null-terminated input
void str_network_to_c(char* string) {
	unsigned int i;
	for (i = 0; i < strlen(string); i++) {
		if (string[i] == '\n' || string[i] == '\r') {
			string[i] = '\0';
			return;
		}
	}
}

void wait_ok() {
	if (syn_ok) {
		char read_buffer[input_buffer_size];
	#ifdef _WIN32
		int result = recv(learner_conn_sd, read_buffer, sizeof(read_buffer), 0);
	#elif __gnu_linux__
		int result = read(learner_conn_sd, read_buffer, sizeof(read_buffer));
	#endif
		if (result <= 0) { // either -1 for an error, or 0 if connection is closed properly
			error("expected 'ok' but could not read socket input\n");
		}
		str_network_to_c(read_buffer);
		

		if (strncmp(read_buffer, "ok", sizeof(read_buffer)) == 0) {
			printf("received permission\n");
		} else {
			char buf[1024];
			sprintf(buf, "expected ok, received %s\n",read_buffer);
			error(buf);
		}
	}
}

void send_ok() {
	if (syn_ok) {
		answer("ok\n");
	}
}

#ifdef _WIN32
DWORD WINAPI do_connect(void *arg)
#elif __gnu_linux__
void *do_connect(void *arg)
#endif
{
	send_ok();
	wait_ok();
	if (connect(main_sd, (struct sockaddr *) &serv_addr_struct, sizeof(serv_addr_struct)) < 0) {
        printf("error connecting to server\n");
	} else {
		printf("connected successfully\n");
	}
	main_socket_blocked = 0;
	return 0;
}

void start_connecting_thread() {
	if (!main_socket_blocked) {
#ifdef _WIN32
		socket_thread = CreateThread(NULL, 0, &do_connect, NULL, 0, NULL);
#elif __gnu_linux__
		pthread_create(&socket_thread, NULL, &do_connect, NULL);
#else
		printf("cannot accept process, unknown OS\n");
#endif
	} else {
		send_ok();
		wait_ok();
	}
	main_socket_blocked = 1;
}

void stop_thread() {
	if (main_socket_blocked) {
#ifdef _WIN32
		TerminateThread(socket_thread, 0);
#elif __gnu_linux__
		pthread_cancel(socket_thread);
#endif
	}
	main_socket_blocked = 0;
}

#ifdef _WIN32
DWORD WINAPI do_accept(void *arg) {
#elif __gnu_linux__
void *do_accept(void *arg) {
#endif
	//do {
	int new_connection = accept(main_sd, (struct sockaddr*)NULL, NULL);
	if (new_connection >= 0) {
		secondary_sd = new_connection;
	}
	if (secondary_sd == -1) {
		printf("accepting failed\n");
		//answer("NOK\n");
	}
	else {
		printf("accepting succeeded\n");
		//answer("OK\n");
	}
	//} while (secondary_sd != -1);
	return 0;
}

void start_accepting_thread() {
	if (!main_socket_blocked) {
#ifdef _WIN32
		socket_thread = CreateThread(NULL, 0, &do_accept, NULL, 0, NULL);
#elif __gnu_linux__
		pthread_create(&socket_thread, NULL, &do_accept, NULL);
#else
		printf("cannot accept process, unknown OS\n");
#endif
	}
	main_socket_blocked = 1;
}

void init() {
	#ifdef _WIN32
		WSADATA wsaData;
		WSAStartup(0x0202, &wsaData);
	#endif
	learner_listener_sd = socket(AF_INET, SOCK_STREAM, 0);
	int reuse = 1;
    if (setsockopt(	learner_listener_sd, SOL_SOCKET, SO_REUSEADDR, (const char*)&reuse, sizeof(reuse)) < 0)
        perror("setsockopt(SO_REUSEADDR) failed");
	struct sockaddr_in learner_addr;
	memset(&learner_addr, 0, sizeof(struct sockaddr_in));
	learner_addr.sin_family = AF_INET; 
	learner_addr.sin_addr.s_addr = htonl(INADDR_ANY);
	learner_addr.sin_port = htons(learner_port);
	bind(learner_listener_sd, (struct sockaddr*)&learner_addr, sizeof(learner_addr));
	
	if (listen(learner_listener_sd, 10) != 0) {
		error("cannot open socket to listen to learner\n");
	}
	printf("listening for learner...\n");
	learner_conn_sd = accept(learner_listener_sd, (struct sockaddr*)NULL, NULL);
	while (learner_conn_sd == -1) {
		printf("could not establish connection with learner, retrying...\n");
		sleep(1);
		learner_conn_sd = accept(learner_listener_sd, (struct sockaddr*)NULL, NULL);
	}
	printf("established connection with learner!\n");
}

void init_run() {
	main_socket_blocked = 0;
	
	printf("*** NEW RUN ***\n");
	printf("creating socket...\n");
	main_sd = socket(AF_INET, SOCK_STREAM, 0);
	printf("finished creating socket!\n");
	if (main_sd < 0) {
		printf("creating socket failed\n");
		error("could not open new socket\n");
	}
	
	printf("initializing server address\n");
	struct hostent *server = gethostbyname(server_addr);
	memset((char *)&serv_addr_struct, 0, sizeof(serv_addr_struct));
    serv_addr_struct.sin_family = AF_INET;
    memcpy((char *)&serv_addr_struct.sin_addr.s_addr,
		(char *)server->h_addr,
		server->h_length);
    serv_addr_struct.sin_port = htons(server_port);

	printf("initializing client address\n");
	memset(&local_addr, 0, sizeof(struct sockaddr_in));
    local_addr.sin_family = AF_INET;
    local_addr.sin_port = htons(0);
    
	int int_addr = inet_pton(AF_INET, "0.0.0.0", &local_addr.sin_addr.s_addr);
	if (int_addr <= 0) {
		if (int_addr == 0)
			error("Client address not a valid address");
		else
			error("Could not convert address");
	}
    
    int assigned_port;
    if (bind(main_sd, (struct sockaddr *)&local_addr, sizeof(struct sockaddr)) == -1) {
		error("could not bind local socket to random port number");
	} else {
		struct sockaddr_in infoaddr;
		socklen_t infolen = sizeof(infoaddr);
		getsockname(main_sd, (struct sockaddr*) &infoaddr, &infolen);
		//(struct sockaddr_in*) infoaddr_pointer = (struct sockaddr_in*) infoaddr;
		assigned_port = ntohs(infoaddr.sin_port);
		printf("bound to randomly assigned port %i\n", assigned_port);
	}
	char buf[100];
	sprintf(buf, "port %i\n", assigned_port);
	answer(buf);
	printf("\nfinished initialization!\n");
}

void close_run() {
#ifdef _WIN32
	closesocket(main_sd);
	closesocket(secondary_sd);
	if (main_sd_old != -1) {
		closesocket(main_sd_old);
	}
	if (secondary_sd_old != -1) {
		closesocket(secondary_sd_old);
	}
#elif __gnu_linux__	
	close(main_sd);
	close(secondary_sd);
	if (main_sd_old != -1) {
		close(main_sd_old);
	}
	if (secondary_sd_old != -1) {
		close(secondary_sd_old);
	}
#endif
	main_sd_old = main_sd;
	secondary_sd_old = secondary_sd;
	main_sd = secondary_sd = -1;
}

void process_connect() {
	//stop_connecting_thread();
	start_connecting_thread();
}

void process_close() {
	send_ok();
	wait_ok();
	stop_thread();
#ifdef _WIN32
	closesocket(main_sd);
#elif __gnu_linux__	
	close(main_sd);
#endif
}

void process_close_secondary() {
	send_ok();
	wait_ok();
	stop_thread();
#ifdef _WIN32
	closesocket(secondary_sd);
#elif __gnu_linux__	
	close(secondary_sd);
#endif
}

void process_accept() {
	//stop_accepting_thread();
	printf("ACCEPT\n");
	start_accepting_thread();
	printf("accepting\n");
}

void process_listen() {
	printf("LISTEN\n");
	if (listen(main_sd, 10) == 0) {
		//answer("OK\n");
		printf("listening succesfully\n");
	}
	else {
		//answer("NOK\n");
		printf("listening failed\n");
#ifdef __gnu_linux__
		int sendbuf;
		socklen_t sendbufsize = sizeof(sendbuf);
		int error = getsockopt(main_sd, SOL_SOCKET, SO_ERROR, &sendbuf, &sendbufsize);
		printf("error-code: %i or %i\n", error, sendbuf);
#elif _WIN32
		printf("no error-code for windows\n");
#endif
	}
}

int process_input() {
	char read_buffer[input_buffer_size];
#ifdef _WIN32
	int result = recv(learner_conn_sd, read_buffer, sizeof(read_buffer), 0);
#elif __gnu_linux__
	int result = read(learner_conn_sd, read_buffer, sizeof(read_buffer));
#endif
	if (result <= 0) { // either -1 for an error, or 0 if connection is closed properly
		return -1;
	}
	str_network_to_c(read_buffer);
	printf("received: %s\n", read_buffer);

	if (strncmp(read_buffer, "connect", sizeof(read_buffer)) == 0) {
		process_connect();
	}
	else if (strncmp(read_buffer, "close", sizeof(read_buffer)) == 0) {
		process_close();
	}
	else if (strncmp(read_buffer, "listen", sizeof(read_buffer)) == 0) {
		process_listen();
	}
	else if (strncmp(read_buffer, "accept", sizeof(read_buffer)) == 0) {
		process_accept();
	}
	else if (strncmp(read_buffer, "closeconnection", sizeof(read_buffer)) == 0) {
		process_close_secondary();
	}
	
	else if (strncmp(read_buffer, "reset", sizeof(read_buffer)) == 0) {
		close_run();
		send_ok();
		init_run();
	}
	else if (strncmp(read_buffer, "port", sizeof("port")-1) == 0) {
		char port_buf[100];
		strncpy(port_buf, &(read_buffer[sizeof("port")]), sizeof(port_buf));
		server_port = atoi(port_buf);
		printf("learner port set to %i\n", server_port);
	}
	else if (strncmp(read_buffer, "exit", sizeof(read_buffer)) == 0) {
		return -1;
	}
	else {
		printf("Unrecognized command %s. Exiting...", read_buffer);
		return -1;
	}
	return 0;
}

void run() {
	init_run();
	
	while(process_input() != -1); // stop if not succesfull, e.g. learner socket has closed.
	
	printf("learner disconnected, terminating\n");
	
	close_run();
	
	#ifdef _WIN32
		closesocket(learner_conn_sd);
	#elif __gnu_linux__
		close(learner_conn_sd);
	#endif
}

char* help = "[-c | --continuous] [-l learnerport] [--dport|-p portnumber] [--daddr|-a ip address]";
int main(int argc, char *argv[]) {
	learner_port = -1;
	learner_listener_sd = learner_conn_sd = main_sd = secondary_sd = main_sd_old = secondary_sd_old = -1;
	int arg_nr;
	int continuous = 0;
	strcpy_end(server_addr, default_server_addr, sizeof(server_addr));
	server_port = default_server_port;
	learner_port = default_learner_port;
	server_port = default_server_port;
	
	for (arg_nr = 1; arg_nr < argc; arg_nr++) {
		if (strcmp(argv[arg_nr], "--continuous") == 0 || strcmp(argv[arg_nr], "-c") == 0) {
			continuous = 1;
		} else if (strcmp(argv[arg_nr], "--learnerport") == 0 || strcmp(argv[arg_nr], "-l") == 0) {
			arg_nr++;
			if (arg_nr >= argc) {
				printf("argument %s needs extra parameter\n", argv[arg_nr-1]);
				return 2;
			}
			learner_port = atoi(argv[arg_nr]);
		} else if (strcmp(argv[arg_nr], "--dport") == 0 || strcmp(argv[arg_nr], "-p") == 0) {
			arg_nr++;
			if (arg_nr >= argc) {
				printf("argument %s needs extra parameter\n", argv[arg_nr-1]);
				return 2;
			}
			server_port = atoi(argv[arg_nr]);
		} else if (strcmp(argv[arg_nr], "--daddr") == 0 || strcmp(argv[arg_nr], "-a") == 0) {
			arg_nr++;
			if (arg_nr >= argc) {
				printf("argument %s needs extra parameter\n", argv[arg_nr-1]);
				return 2;
			}
			strcpy_end(server_addr, argv[arg_nr], sizeof(server_addr));
		} else {
			printf("unknown command line argument %s\nusage:\n%s\n", argv[arg_nr], help);
			return 1;
		}
	}
	printf("listening for learner on port %i, learning on server %s:%i\n", learner_port, server_addr, server_port);
	if (continuous) {
		printf("listening continuously, just kill when not needed anymore\n");
	}
	init();
	// if continuous, keep running, otherwise run once
	do {
		run();
	} while (continuous);
	
	#ifdef _WIN32
		closesocket(learner_listener_sd);
		WSACleanup();
	#elif __gnu_linux__	
		close(learner_listener_sd);	
	#endif
	return 0;
}
