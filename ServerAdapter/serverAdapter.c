#include <string.h>
#include <stdio.h>

#ifdef __gnu_linux__
#include <sys/types.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <pthread.h>
#include <unistd.h>
#include <stdlib.h>
#elif _WIN32
#include <winsock.h>
#include <windows.h>
#pragma comment(lib, "wsock32.lib")
#else
#error OS not supported, have fun with adding ifdefs
#endif


int learner_listener_sd, learner_conn_sd, server_sd, conn_sd;
struct sockaddr_in learner_addr, server_addr;
#ifdef _WIN32
HANDLE accepting_thread;
#elif __gnu_linux__
pthread_t accepting_thread;
#endif

int accepting;
int min_server_port, max_server_port, learner_port, server_port;
#define default_learner_port 5000
#define default_min_server_port 20000
#define default_max_server_port 30000

#define input_buffer_size 1025
#define output_buffer_size 1025

char output_buffer[output_buffer_size];

void start_accepting_thread();
void stop_accepting_thread();

void init() {
#ifdef _WIN32
	WSADATA wsaData;
	WSAStartup(0x0202, &wsaData);
#endif

	server_addr.sin_family = AF_INET;
	server_addr.sin_addr.s_addr = htonl(INADDR_ANY);

	learner_listener_sd = socket(AF_INET, SOCK_STREAM, 0);
	learner_addr.sin_family = AF_INET;
	learner_addr.sin_addr.s_addr = htonl(INADDR_ANY);
	learner_addr.sin_port = htons(learner_port);
	bind(learner_listener_sd, (struct sockaddr*)&learner_addr, sizeof(learner_addr));

	listen(learner_listener_sd, 10);
}

void pstrcpy(char *output_buffer, int buffer_size, char *source) {
#ifdef _WIN32
	strcpy_s(output_buffer, buffer_size, source);
#elif __gnu_linux
	strcpy(output_buffer, output);
#endif
}

// we don't need a more general portable function
void psprintf(char *output_buffer, int buffer_size, const char *format, int integer) {
#ifdef _WIN32
	sprintf_s(output_buffer, buffer_size, format, integer);
#elif __gnu_linux
	sprintf(output_buffer, buffer_size, format, integer);
#endif
}

void answer(char* output) {
	pstrcpy(output_buffer, output_buffer_size, output);
#ifdef _WIN32
    if (send(learner_conn_sd, output_buffer, strlen(output), 0) == SOCKET_ERROR) {
		printf("Error code %d", WSAGetLastError());
	}
#elif __gnu_linux
	if (send(learner_conn_sd, output_buffer, strlen(output), 0) == -1) {
		printf("Failed to send %s", output_buffer);
	}
#endif
}

void init_run() {
	accepting = 0;
	server_sd = socket(AF_INET, SOCK_STREAM, 0);

	server_port++;
	if (server_port > max_server_port) {
		server_port = min_server_port;
	}

	server_addr.sin_port = htons(server_port);
	bind(server_sd, (struct sockaddr*)&server_addr, sizeof(server_addr));
	printf("*** NEW RUN ***\nusing port %i\n", server_port);
	char output[100];
	psprintf(output, 100, "port %i\n", server_port);
	answer(output);
}

void close_run() {
	stop_accepting_thread();

#ifdef _WIN32
	closesocket(server_sd);
	closesocket(conn_sd);
#elif __gnu_linux__	
	close(server_sd);
	close(conn_sd);
#endif
}

void str_network_to_c(char* string) {
	unsigned int i;
	for (i = 0; i < strlen(string); i++) {
		if (string[i] == '\n' || string[i] == '\r') {
			string[i] = '\0';
			return;
		}
	}
}

#ifdef _WIN32
DWORD WINAPI do_accept(void *arg) {
#elif __gnu_linux__
void *do_accept(void *arg) {
#endif
	//do {
	int new_connection = accept(server_sd, (struct sockaddr*)NULL, NULL);
	if (new_connection >= 0) {
		conn_sd = new_connection;
	}
	if (conn_sd == -1) {
		printf("accepting failed\n");
		answer("NOK\n");
	}
	else {
		printf("accepting succeeded\n");
		answer("OK\n");
	}
	//} while (conn_sd != -1);
	return 0;
}

void process_listen() {
	printf("LISTEN\n");
	if (listen(server_sd, 10) == 0) {
		answer("OK\n");
		printf("listening succesfully\n");
	}
	else {
		answer("NOK\n");
		printf("listening failed\n");
#ifdef __gnu_linux__
		int sendbuf;
		socklen_t sendbufsize = sizeof(sendbuf);
		int error = getsockopt(server_sd, SOL_SOCKET, SO_ERROR, &sendbuf, &sendbufsize);
		printf("error-code: %i or %i\n", error, sendbuf);
#elif _WIN32
		printf("but no error-code for you, my windows friend!\n");
#endif
	}
}

void start_accepting_thread() {
	if (!accepting) {
#ifdef _WIN32
		accepting_thread = CreateThread(NULL, 0, &do_accept, NULL, 0, NULL);
#elif __gnu_linux__
		pthread_create(&accepting_thread, NULL, &do_accept, NULL);
#else
		printf("cannot accept process, unknown OS\n");
#endif
	}
	accepting = 1;
}

void stop_accepting_thread() {
	if (accepting) {
#ifdef _WIN32
		TerminateThread(accepting_thread, 0);
#elif __gnu_linux__
		pthread_cancel(accepting_thread);
#endif
	}
	accepting = 0;
}

void process_accept() {
	stop_accepting_thread();
	printf("ACCEPT\n");
	start_accepting_thread();
	printf("accepting\n");
}

void process_close_server() {
	printf("CLOSE SERVER\n");
#ifdef _WIN32
	if (closesocket(server_sd) == 0) {
#elif __gnu_linux__
	if (close(server_sd) == 0) {
#endif
		answer("OK\n");
		printf("close successful\n");
	}
	else {
		answer("NOK\n");
		printf("close failed\n");
#ifdef __gnu_linux__
		int sendbuf;
		socklen_t sendbufsize = sizeof(sendbuf);
		int error = getsockopt(server_sd, SOL_SOCKET, SO_ERROR, &sendbuf, &sendbufsize);
		printf("error-code: %i or %i\n", error, sendbuf);
#elif _WIN32
		printf("but no error-code for you, my windows friend!\n");
#endif
	}
	}

void process_close_connection() {
	printf("CLOSE CONNECTION\n");
	if (conn_sd != -1) {
#ifdef _WIN32
		if (closesocket(conn_sd) == 0) {
#elif __gnu_linux__
		if (close(conn_sd) == 0) {
#endif
			conn_sd = -1;
			answer("OK\n");
			printf("close succesful\n");
		}
		else {
			answer("NOK\n");
			printf("close failed\n");
		}
		}
	else {
		answer("NOK\n");
		printf("no connection to close!\n");
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

	if (strncmp(read_buffer, "listen", sizeof(read_buffer)) == 0) {
		process_listen();
	}
	else if (strncmp(read_buffer, "accept", sizeof(read_buffer)) == 0) {
		process_accept();
	}
	else if (strncmp(read_buffer, "closeserver", sizeof(read_buffer)) == 0) {
		process_close_server();
	}
	else if (strncmp(read_buffer, "closeconnection", sizeof(read_buffer)) == 0) {
		process_close_connection();
	}
	else if (strncmp(read_buffer, "reset", sizeof(read_buffer)) == 0) {
		close_run();
		init_run();
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
	printf("now listening for learner...\n");
	learner_conn_sd = accept(learner_listener_sd, (struct sockaddr*)NULL, NULL);

	printf("learner connected!");

	init_run();

	while (process_input() != -1); // stop if not succesfull, e.g. learner socket has closed.

	printf("learner disconnected, terminating\n");

	close_run();

#ifdef _WIN32
	closesocket(learner_conn_sd);
#elif __gnu_linux__
	close(learner_conn_sd);
#endif
}

char* help = "[-c | --continuous] [-l learnerport] [-m minport] [-n maxport]";

int main(int argc, char *argv[]) {
	learner_port = min_server_port = max_server_port = -1;
	learner_listener_sd = learner_conn_sd = server_sd = conn_sd = -1;
	int arg_nr;
	int continuous = 0;
	for (arg_nr = 1; arg_nr < argc; arg_nr++) {
		if (strcmp(argv[arg_nr], "--continuous") == 0 || strcmp(argv[arg_nr], "-c") == 0) {
			continuous = 1;
		}
		else if (strcmp(argv[arg_nr], "--learnerport") == 0 || strcmp(argv[arg_nr], "-l") == 0) {
			arg_nr++;
			if (arg_nr >= argc) {
				printf("argument %s needs extra parameter\n", argv[arg_nr - 1]);
				return 2;
			}
			learner_port = atoi(argv[arg_nr]);
		}
		else if (strcmp(argv[arg_nr], "--minserverport") == 0 || strcmp(argv[arg_nr], "-m") == 0) {
			arg_nr++;
			if (arg_nr >= argc) {
				printf("argument %s needs extra parameter\n", argv[arg_nr - 1]);
				return 2;
			}
			min_server_port = atoi(argv[arg_nr]);
		}
		else if (strcmp(argv[arg_nr], "--maxserverport") == 0 || strcmp(argv[arg_nr], "-n") == 0) {
			arg_nr++;
			if (arg_nr >= argc) {
				printf("argument %s needs extra parameter\n", argv[arg_nr - 1]);
				return 2;
			}
			max_server_port = atoi(argv[arg_nr]);
		}
		else {
			printf("unknown command line argument %s\nusage:\n%s\n", argv[arg_nr], help);
			return 1;
		}
	}
	if (min_server_port < 0) {
		min_server_port = default_min_server_port;
	}
	if (max_server_port < 0) {
		max_server_port = default_max_server_port;
	}
	if (learner_port < 0) {
		learner_port = default_learner_port;
	}
	server_port = min_server_port;
	printf("listening for learner on port %i, learning on portrange %i-%i\n", learner_port, min_server_port, max_server_port);
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
