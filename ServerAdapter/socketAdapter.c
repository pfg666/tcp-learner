#include <stdio.h>
#include <string.h>
#include <errno.h>
#ifdef __gnu_linux__
	#include <sys/types.h>
	#include <sys/socket.h>
	#include <netinet/in.h>
	#include <unistd.h>
	#include <stdlib.h>
	#include <netdb.h>
	#include <arpa/inet.h>
	#include <pthread.h>
	#include <netinet/tcp.h>
#elif _WIN32
	#include <winsock2.h>
	#include <windows.h>
	#include <ws2tcpip.h>
	#pragma comment(lib, "Ws2_32.lib ")
	#pragma comment(lib, "wsock32.lib")
#else
	#error OS not supported, have fun with adding ifdefs
#endif

int learner_listener_sd, learner_conn_sd, main_sd, secondary_sd;
#ifdef _WIN32
HANDLE socket_thread;
#elif __gnu_linux__
pthread_t socket_thread;
#endif
int main_socket_blocked;

int server_port, learner_port;
char server_addr[100];

#define send_buf "x"

#define default_learner_port 5000
#define default_server_port 34567
#define default_server_addr "1.2.3.4"

#define input_buffer_size 1025
#define output_buffer_size 1025
char output_buffer[output_buffer_size];

#define syn_ok (0)
#define client_type (0)
#define server_type (1)
int type;

struct sockaddr_in serv_addr_struct;
struct sockaddr_in local_addr;

void error(char* msg) {
	printf("error: %s\naborting\n", msg);
	exit(-1);
}

void print_errno() {
	if (errno == EBADF) {
		printf("errno = EBADF\n");
	} else if (errno == EINTR) {
		printf("errno = EINTR\n");
	} else if (errno == EIO) {
		printf("errno = EIO\n");
	} else {
		printf("errno = %i\n", errno);
	}
}

void print_socket_status(int sd) {
	int error = 0;
	socklen_t len = sizeof(error);
	int retval = getsockopt(sd, SOL_SOCKET, SO_ERROR, &error, &len);
	
	if (retval != 0) {
		/* there was a problem getting the error code */
		printf("error getting socket error code: %s\n", strerror(retval));
		return;
	} else {
		printf("socket alive\n");
		return;
	}

	if (error != 0) {
		/* socket has a non zero error status */
		fprintf(stderr, "socket error: %s\n", strerror(error));
	}
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
	#ifdef _WIN32
		strcpy_s(dest, len, src);
	#else
		strncpy(dest, src, len - 1);
	#endif
	dest[len-1] = '\0';
}

void answer(char* output) {
	if (send(learner_conn_sd, output, strlen(output), 0) == -1) {
		printf("Could not send '%s'\n", output);
		print_errno();
	}
}

// convert first newline in input to null-terminator. Returns string
// length of the new string, or -1 if no newline was found or if a null-
// terminator was found first
int str_network_to_c(char* string, int len) {
	int i;
	for (i = 0; i < len; i++) {
		if (string[i] == '\n' || string[i] == '\r') {
			string[i] = '\0';
			return i;
		} else if (string[i] == '\0') {
			return -1;
		}
	}
	return -1;
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
		str_network_to_c(read_buffer, sizeof(read_buffer));

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
		int i = 1;
		setsockopt(main_sd, IPPROTO_TCP, TCP_NODELAY, (void *)&i, sizeof(i));
		#ifdef __gnu_linux__
			setsockopt(main_sd, IPPROTO_TCP, TCP_QUICKACK, (void *)&i, sizeof(i));
		#endif
	}
	main_socket_blocked = 0;
	return 0;
}

void start_connecting_thread() {
	if (main_socket_blocked == 0 && secondary_sd == -1) {
		main_socket_blocked = 1;
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
		int i = 1;
		setsockopt(secondary_sd, IPPROTO_TCP, TCP_NODELAY, (void *)&i, sizeof(i));
		#ifdef __gnu_linux__
			setsockopt(secondary_sd, IPPROTO_TCP, TCP_QUICKACK, (void *)&i, sizeof(i));
		#endif
		printf("accepting succeeded\n");
		//answer("OK\n");
	}
	//} while (secondary_sd != -1);
	main_socket_blocked = 0;
	return 0;
}

void start_accepting_thread() {
	if (main_socket_blocked == 0 && secondary_sd == -1) {
		main_socket_blocked = 1;
#ifdef _WIN32
		socket_thread = CreateThread(NULL, 0, &do_accept, NULL, 0, NULL);
#elif __gnu_linux__
		pthread_create(&socket_thread, NULL, &do_accept, NULL);
#else
		printf("cannot accept process, unknown OS\n");
#endif
	}
}

void init() {
	#ifdef _WIN32
		WSADATA wsaData;
		WSAStartup(0x0202, &wsaData);
	#endif
	learner_listener_sd = socket(AF_INET, SOCK_STREAM, 0);
	struct sockaddr_in learner_addr;
	memset(&learner_addr, 0, sizeof(struct sockaddr_in));
	learner_addr.sin_family = AF_INET; 
	learner_addr.sin_addr.s_addr = htonl(INADDR_ANY);
	learner_addr.sin_port = htons(learner_port);
	int yes = 1;
	setsockopt(learner_listener_sd, SOL_SOCKET, SO_REUSEADDR, &yes, sizeof(int));
	bind(learner_listener_sd, (struct sockaddr*)&learner_addr, sizeof(learner_addr));
	
	if (listen(learner_listener_sd, 1) != 0) {
		error("cannot open socket to listen to learner\n");
	}
	printf("listening for learner...\n");
	learner_conn_sd = accept(learner_listener_sd, (struct sockaddr*)NULL, NULL);
	while (learner_conn_sd == -1) {
		printf("could not establish connection with learner, retrying...\n");
		#ifdef _WIN32
		Sleep(1);
		#elif __gnu_linux__
		sleep(1);
		#endif
		learner_conn_sd = accept(learner_listener_sd, (struct sockaddr*)NULL, NULL);
	}
	printf("established connection with learner!\n");
}

void init_run() {
	main_socket_blocked = 0;
	//stop_thread();
	type = -1;
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

	#ifdef _WIN32
	int int_addr = InetPton(AF_INET, "0.0.0.0", &local_addr.sin_addr.s_addr);
	#elif __gnu_linux__
	int int_addr = inet_pton(AF_INET, "0.0.0.0", &local_addr.sin_addr.s_addr);
	#endif
	
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
		int infolen = sizeof(infoaddr);
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
#elif __gnu_linux__	
	if (main_sd != -1 && close(main_sd) != 0) {
		error("could not close main socket");
		print_errno();
	}
	if (secondary_sd != -1 && close(secondary_sd) != 0) {
		error("could not close secondary socket");
		print_errno();
	}
#endif
	main_sd = secondary_sd = -1;
}

void process_connect() {
	type = client_type;
	//stop_connecting_thread();
	printf("now starting connecting thread\n");
	start_connecting_thread();
	printf("started connecting thread\n");
}

void process_send() {
	int sd = -1;
	// find the connection socket, if any
	if (type == server_type) {
		sd = secondary_sd;
	} else if (type == client_type) {
		sd = main_sd;
	}
	
	if (sd != -1) {
		printf("starting send on socket %i, type %s\n", sd, type == server_type ? "server" : type == client_type ? "client" : "?");
		//printf("socket fd's:\nlearner listener %i\nlearner connection %i\nmain %i\nsecondary %i\n", learner_listener_sd, learner_conn_sd, main_sd, secondary_sd);
		//send(SOCKET socket, const char * buffer, int buflen, int flags);
		//printf("learner connection status:\n");
		//print_socket_status(learner_conn_sd);
		//printf("SUT socket status:\n");
		//print_socket_status(sd);
		//printf("learner listener socket status:\n");
		//print_socket_status(learner_listener_sd);
		//printf("learner connection socket status:\n");
		//print_socket_status(learner_conn_sd);
		printf("now sending '%s' with length %i\n", send_buf, (int)strlen(send_buf));
		send(sd, send_buf, strlen(send_buf), MSG_DONTWAIT);
	} else {
		printf("cannot send on sd=-1");
	}
	printf("finishing send\n");
}

void process_rcv() {
	int sd = -1;
	if (type == server_type) {
		sd = secondary_sd;
	} else if (type == client_type) {
		sd = main_sd;
	}
	if (sd != -1) {
		char read_buffer[input_buffer_size];
		recv(sd, read_buffer, sizeof(read_buffer), MSG_DONTWAIT);
	}
}

void process_close() {
	send_ok();
	wait_ok();
	stop_thread();
	char read_buffer[200];
	//recv(main_sd, read_buffer, sizeof(read_buffer), 0);
#ifdef _WIN32
	closesocket(main_sd);
#elif __gnu_linux__	
	close(main_sd);
#endif
	main_sd = -1;
}

void process_close_secondary() {
	send_ok();
	wait_ok();
	stop_thread();
	//char read_buffer[200];
	//recv(secondary_sd, read_buffer, sizeof(read_buffer), 0);
#ifdef _WIN32
	closesocket(secondary_sd);
#elif __gnu_linux__	
	close(secondary_sd);
#endif
	secondary_sd = -1;
}

void process_accept() {
	//stop_accepting_thread();
	printf("ACCEPT\n");
	start_accepting_thread();
	printf("accepting\n");
}

void process_listen() {
	type = server_type;
	printf("LISTEN\n");
	if (listen(main_sd, 1) == 0) {
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

// shifts out the first string in a buffer, so that a buffer
// string1\0string2 so that it becomes string2 (followed by garbage)
void shift_out_first_string(char buffer[], int buf_size) {
	int first_str_len = strlen(buffer);
	int i;
	for (i = 0; i + first_str_len + 1 < buf_size; i++) {
		buffer[i] = buffer[i + first_str_len + 1];
	}
}

char learner_input[input_buffer_size];
int unprocessed_learner_input = 0;
// Returns 0 if learning should continue, -1 if it stops normally
// (e.g. through an exit command), any other value if it stops abnormally
int process_input() {
	int retVal = 0;
	if (unprocessed_learner_input == 0) {
	#ifdef _WIN32
		int result = recv(learner_conn_sd, learner_input, sizeof(learner_input), 0);
	#elif __gnu_linux__
		int result = read(learner_conn_sd, learner_input, sizeof(learner_input));
	#endif
		if (result <= 0) { // either -1 for an error, or 0 if connection is closed properly
			printf("could not read next input from learner\n");
			print_errno();
			return 1;
		} else {
			printf("Received new command data '%.*s'\n", result, learner_input);
			unprocessed_learner_input = result;
		}
	}
	int cmd_length = str_network_to_c(learner_input, unprocessed_learner_input);
	if (cmd_length == -1) {
		learner_input[sizeof(learner_input) - 1] = '\0';
		printf("Could not process input, no newline found. Data:\n'%s'", learner_input);
		return 1;
	}
	printf("processing: '%s'\n", learner_input);
	
	if (strncmp(learner_input, "connect", sizeof(learner_input)) == 0) {
		process_connect();
	} else if (strncmp(learner_input, "close", sizeof(learner_input)) == 0) {
		process_close();
	} else if (strncmp(learner_input, "listen", sizeof(learner_input)) == 0) {
		process_listen();
	} else if (strncmp(learner_input, "accept", sizeof(learner_input)) == 0) {
		process_accept();
	} else if (strncmp(learner_input, "closeconnection", sizeof(learner_input)) == 0) {
		process_close_secondary();
	} else if (strncmp(learner_input, "send", sizeof(learner_input)) == 0) {
		process_send();
	} else if (strncmp(learner_input, "rcv", sizeof(learner_input)) == 0) {
		process_rcv();
	} else if (strncmp(learner_input, "reset", sizeof(learner_input)) == 0) {
		printf("closing run\n");
		close_run();
		send_ok();
		printf("init run\n");
		init_run();
		printf("finished init run\n");
	/*} else if (strncmp(learner_input, "port", sizeof("port")-1) == 0) {
		char port_buf[100];
		strncpy(port_buf, &(read_buffer[sizeof("port")]), sizeof(port_buf));
		server_port = atoi(port_buf);
		printf("learner port set to %i\n", server_port);*/
	}
	else if (strncmp(learner_input, "exit", sizeof(learner_input)) == 0) {
		retVal = -1;
	}
	else if (strncmp(learner_input, "probe", sizeof(learner_input)) == 0) {
		printf("ignoring probe\n");
	}
	else {
		printf("Unrecognized command %s. Exiting...", learner_input);
		retVal = 1;
	}
	
	// move any unread data to the front of the buffer
	int processed_learner_input = strlen(learner_input) + 1;
	unprocessed_learner_input -= processed_learner_input;
	int shifting = unprocessed_learner_input > 0;
	int i;
	if (shifting) {
		printf("already received next input, shifting it forward\n");
		shift_out_first_string(learner_input, sizeof(learner_input));
	}
	if (shifting) {
		printf("shifted to '%s'\n", learner_input);
	}
	return retVal;
}

int run() {
	init_run();
	
	int returnCode;
	while((returnCode = process_input()) == 0); // stop if not succesfull, e.g. learner socket has closed.
	
	printf("learner disconnected, terminating\n");
	
	close_run();
	
	#ifdef _WIN32
		closesocket(learner_conn_sd);
	#elif __gnu_linux__
		close(learner_conn_sd);
	#endif
	
	return returnCode;
}

char* help = "[-c | --continuous] [-l learnerport] [--dport|-p portnumber] [--daddr|-a ip address]";
int main(int argc, char *argv[]) {
	learner_port = -1;
	learner_listener_sd = learner_conn_sd = main_sd = secondary_sd = -1;
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
	int returnCode;
	do {
		returnCode = run();
	} while (continuous);
	
	#ifdef _WIN32
		closesocket(learner_listener_sd);
		WSACleanup();
	#elif __gnu_linux__	
		close(learner_listener_sd);	
	#endif
	if (returnCode == -1) {
		return 0;
	} else {
		return returnCode;
	}
}
