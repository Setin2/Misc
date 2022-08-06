#include <fuse.h>
#include <errno.h>
#include <string.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <sys/types.h>
#include <time.h>
#include <syslog.h>

int lfs_getattr( const char *, struct stat * );
int lfs_readdir( const char *, void *, fuse_fill_dir_t, off_t, struct fuse_file_info * );
int lfs_mknod( const char *path, mode_t mode, dev_t rdev );
int lfs_unlink( const char*path);
int lfs_mkdir( const char *path, mode_t mode );
int lfs_rmdir( const char *path );
int lfs_open( const char *, struct fuse_file_info *);
int lfs_read( const char *, char *, size_t, off_t, struct fuse_file_info * );
int lfs_release(const char *path, struct fuse_file_info *fi);
int lfs_write( const char *path, const char *buffer, size_t size, off_t offset, struct fuse_file_info *info );

static struct fuse_operations lfs_oper = {
	.getattr =      lfs_getattr,
	.readdir =      lfs_readdir,
	.mknod = lfs_mknod,
	.mkdir = lfs_mkdir,
	.unlink = lfs_unlink,
	.rmdir = lfs_rmdir,
	.truncate = NULL,
	.open   = lfs_open,
	.read   = lfs_read,
	.release = lfs_release,
	.write = lfs_write,
	.rename = NULL,
	.utime = NULL
};

#define MAX_NAME_SIZE 256 // maximum size for directory/file names
#define MAX_FILE_SIZE 256 // maximum size of a file (bytes)
#define MAX_DIR_SIZE 256 // maximum number of elements in a directory
#define MAX_NR_DIR 256 // maximum number of directories/files in the file system

/*
 * Structure representing a directory
 * Unlike with a file, the size attribute represents the number of subdirectories/files in it
 */
struct Directory {
	char name[MAX_NAME_SIZE];
	char parent[MAX_NAME_SIZE];
	char files[MAX_DIR_SIZE][MAX_NAME_SIZE];
	int size;
};

/*
 * Structure representing a file
 */
struct File {
	char name[MAX_NAME_SIZE];
	char parent[MAX_NAME_SIZE];
	char content[MAX_FILE_SIZE];
	int size;
};

struct Directory home; // the root directory

struct Directory dir_list[MAX_NR_DIR]; // all other directories then the root directory
int dir_list_size = -1;

struct File files_list[MAX_NR_DIR]; // all the files in the system
int files_list_size = -1;

/*
 * Directories are named as .../parent/directory
 * This function removes everything before the directory substring and returns it
 */
char *get_subdirectory(char buff[], int n){
	// we count the number of "/" characters
	int i, count;
	for (i = 0, count = 0; buff[i]; i++)
		count += (buff[i] == '/');

	// remove everything before the last subdirectory in the string
	char delim[] = "/";
	char *cwd = strtok(buff, delim);
	for (int i = 0; i < count - n; i++){
		cwd = strtok(NULL, delim);
	}

	return cwd;
}

/*
 * Returns 1 if the given string is the name of one of the directories in the list of directories
 */
int is_dir( const char *path , int n){
	// we need to reove one "/" character
	if (n == 0 ){
		path++;
		for ( int curr_idx = 0; curr_idx <= dir_list_size; curr_idx++ )
			if ( strcmp( path, dir_list[ curr_idx ].name ) == 0 )
				return 1;
	// we need to remove a bigger substring
	} else {
		for ( int curr_idx = 0; curr_idx <= dir_list_size; curr_idx++ ){

			char tmp[sizeof(dir_list[ curr_idx ].name) + 1];
			strcpy(tmp, dir_list[ curr_idx ].name);
			char *new_dir_name = get_subdirectory(tmp, 0);

			if ( strcmp( path, new_dir_name) == 0 )
				return 1;
		}
	}
    return 0;
}

/*
 * Returns the index of the given directory or -1
 */
int get_dir_index( const char *path, int n){
	if (n == 0){
		path++;
		for ( int curr_idx = 0; curr_idx <= dir_list_size; curr_idx++ )
			if ( strcmp( path, dir_list[ curr_idx ].name ) == 0 )
				return curr_idx;
	} else {
		for ( int curr_idx = 0; curr_idx <= dir_list_size; curr_idx++ ){
			
			char tmp[sizeof(dir_list[ curr_idx ].name) + 1];
			strcpy(tmp, dir_list[ curr_idx ].name);
			char *new_dir_name = get_subdirectory(tmp, 0);

			if ( strcmp( path, new_dir_name ) == 0 )
				return curr_idx;
		}
    }
    return -1;
}

/*
 * Returns 1 if the given string is the name of one of the files in the list of files
 */
int is_file( const char *path){
	path++;
	for ( int curr_idx = 0; curr_idx <= files_list_size; curr_idx++ )
		if ( strcmp( path, files_list[ curr_idx ].name ) == 0 )
			return 1;

	return 0;
}

/*
 * Returns the index of the given file or -1
 */
int get_file_index( const char *path ){
	path++;
	for ( int curr_idx = 0; curr_idx <= files_list_size; curr_idx++ )
		if ( strcmp( path, files_list[ curr_idx ].name ) == 0 )
			return curr_idx;

	return -1;
}

/*
 * Stores the name of the current working directory in buff
 */
void lfs_readlink(char buff[]){
	// we get the process id of the current working directory
	struct fuse_context* fc = fuse_get_context();

	// we create a string /proc/"pid"/cwd
	char path[MAX_NAME_SIZE];
	strcpy(path, "/proc/");
	char inttochar[MAX_NAME_SIZE];
	sprintf(inttochar, "%d", fc->pid);
	strcat(path, inttochar);
	strcat(path, "/cwd");

	// we use readlink on the string and store the name of the current working directory in buff
	ssize_t len = readlink(path, buff, MAX_NAME_SIZE-1);
	if (len != -1) {
			buff[len] = '\0';
	}
}

int lfs_getattr( const char *path, struct stat *st ){
	// set access and modification time
	st->st_atime = time( NULL );
	st->st_mtime = time( NULL );
	
	// this is a directory
	if ( strcmp( path, "/" ) == 0 || is_dir(path, 0) == 1 ){
		st->st_mode = S_IFDIR | 0755;
		st->st_nlink = 2;
	}
	// this is a file
	else if ( is_file( path ) == 1 ){
		st->st_mode = S_IFREG | 0644;
		st->st_nlink = 1;
		st->st_size = 1024;
	}
	else {
		return -ENOENT;
	}
    return 0;
}

/*
 * List the subdirectories or files in the current directory
 */
int lfs_readdir( const char *path, void *buffer, fuse_fill_dir_t filler, off_t offset, struct fuse_file_info *fi ){
	(void) fi;

	filler( buffer, ".", NULL, 0 ); // Current Directory
	filler( buffer, "..", NULL, 0 ); // Parent Directory

	// this is the root directory, we print the contetes of home
	if ( strcmp( path, "/" ) == 0 ){
		for ( int i = 0; i <= home.size; i++ )
			filler( buffer, home.files[ i ], NULL, 0 );
	}
	// we find the index of the given directory and print its content
	else {
		int dir_idx = get_dir_index(path, 0);
		if (dir_idx != -1){
			for (int i = 0; i <= dir_list[dir_idx].size; i++){
				char tmp[MAX_NAME_SIZE];
				strcpy(tmp, dir_list[dir_idx].files[i]);
				char *new_dir_name = get_subdirectory(tmp, 0);

				filler( buffer, new_dir_name, NULL, 0 );
			}
		}
	}
	return 0;
}

/* 
 * Read the content of the given file and return the number of bytes read
 */
int lfs_read( const char *path, char *buffer, size_t size, off_t offset, struct fuse_file_info *fi ){
	// open the file
	if (lfs_open(path, fi) == 0){
		int file_idx = get_file_index( path ); // get the index of the file

		if ( file_idx == -1 ) // there is no such file
			return -1;

		char *content = files_list[ file_idx ].content;
		memcpy( buffer, content + offset, size );

		return strlen( content ) - offset; // return the number of bytes read
	}
	// couldnt open the file
    return 0;
}

/*
 * Create a new directory with the given name
 */
int lfs_mkdir( const char *path, mode_t mode ){
	path++;
	// we check wheteher the lenght of the name is small enough
	if (strlen(path) < MAX_NAME_SIZE){
		// get current working directory
		char buff[MAX_NAME_SIZE*10];
		lfs_readlink(buff);
		// we remove the whole substring before the current directory
		char *cwd = get_subdirectory(buff, 1);

		// there is still space left in this directory
		if (dir_list_size < MAX_NR_DIR && dir_list[get_dir_index(cwd, 1)].size < MAX_DIR_SIZE){
			// the current directory is in the list of directories
			if (is_dir(cwd, 1) == 1){
				// increment the size of the list of directories
				dir_list_size++;
				// get the index of the current working directory
				int dir_index = get_dir_index(cwd, 1);
				// we add a new directory inside it so we increment its size as well
				dir_list[dir_index].size++;
				// copy the name of the new dir to the list of directories
				strcpy( dir_list[ dir_list_size ].name, path );
				// add name of the directory to the list of files in the current directory
				strcpy( dir_list[ dir_index ].files[dir_list[ dir_index ].size], path );
				// set the size of the newly constructed dir to -1
				dir_list[dir_list_size].size = -1;
				// set the current directory as the parent of the newly created directory
				strcpy(dir_list[dir_list_size].parent, cwd);
			}
			// this is the root directory
			else {
				dir_list_size++;
				home.size++;
				strcpy( dir_list[ dir_list_size ].name, path );
				strcpy( home.files[home.size], path );
				dir_list[dir_list_size].size = -1;
				strcpy(dir_list[dir_list_size].parent, "home");
			}
			return 0;
		}
	} return -1;
}

/*
 * Create a new file with the given name
 */
int lfs_mknod( const char *path, mode_t mode, dev_t rdev ){
	path++;
	if (strlen(path) < MAX_NAME_SIZE){
		char buff[MAX_NAME_SIZE*10];
		lfs_readlink(buff);
		char *cwd = get_subdirectory(buff, 1);

		if (dir_list_size < MAX_NR_DIR && dir_list[get_dir_index(cwd, 1)].size < MAX_DIR_SIZE){
			if (is_dir(cwd, 1) == 1){
				files_list_size++;
				int dir_index = get_dir_index(cwd, 1);
				dir_list[dir_index].size++;
				strcpy( files_list[ files_list_size ].name, path );
				strcpy( dir_list[ dir_index ].files[dir_list[ dir_index ].size], path );
				files_list[files_list_size].size = -1;
				strcpy(files_list[files_list_size].parent, cwd);
				strcpy(files_list[ files_list_size ].content, "" );
			} else {
				files_list_size++;
				home.size++;
				strcpy( files_list[ files_list_size ].name, path );
				strcpy( home.files[home.size], path );
				files_list[files_list_size].size = -1;
				strcpy(files_list[files_list_size].parent, "home");
				strcpy(files_list[ files_list_size ].content, "" );
			}
			return 0;
		}
	} return -1;
}

/*
 * Write a series of bytes to the given file
 */
int lfs_write( const char *path, const char *buffer, size_t size, off_t offset, struct fuse_file_info *info ){
    if (lfs_open(path, info) == 0){
		int file_idx = get_file_index( path );

		if ( file_idx == -1 ) // No such file
			return 0;

		if (strlen(buffer) < MAX_FILE_SIZE)
			strcpy( files_list[ file_idx ].content, buffer );
	}

    return size;
}

/*
 * Delete the given file
 */
int lfs_unlink( const char *path ){
	// get the index of the given file
	int idx = get_file_index(path);
	// get the index of the parent of the given file
	int p_idx = get_dir_index(files_list[idx].parent, 1);

	// the file resides in the root directory
	if (strcmp(files_list[idx].parent, "home") == 0){
		// we get the index of the given file in the list of files ppertaining to the parent directory (in j)
		int j = -1;
		for (int i = 0; i <= home.size; i++){
			path++;
			if (strcmp(home.files[i], path) == 0){
				j = i;
			}
			path--;
		}
		// we remove the file from the list of files in the parent directory
		int i;
		for (i = j; i <= home.size; i++){
			strcpy(home.files[i], home.files[i+1]);
		}
		home.size--;
		// we get the contents of each file that comes after this one and copy them one space backwards
		for (i = idx; i < files_list_size; i++){
			strcpy(files_list[i].name, files_list[i+1].name);
			strcpy(files_list[i].parent, files_list[i+1].parent);
			strcpy(files_list[i].content, files_list[i+1].content);
			files_list[i].size = files_list[i+1].size;
		}
		// and delete the last file
		memset(&files_list[i], 0, sizeof*files_list);
		files_list_size--;
	// the file resides in any of the other directories
	} else {
		int j = -1;
		for (int i = 0; i <= dir_list[p_idx].size; i++){
			path++;
			if (strcmp(dir_list[p_idx].files[i], path) == 0)
				j = i;
			path--;
		}
		int i;
		for (i = j; i <= dir_list[p_idx].size; i++){
			strcpy(dir_list[p_idx].files[i], dir_list[p_idx].files[i+1]);
		}
		dir_list[p_idx].size--;

		for (i = idx; i < files_list_size; i++){
			strcpy(files_list[i].name, files_list[i+1].name);
			strcpy(files_list[i].parent, files_list[i+1].parent);
			strcpy(files_list[i].content, files_list[i+1].content);
			files_list[i].size = files_list[i+1].size;
		}
		memset(&files_list[i], 0, sizeof*files_list);
		files_list_size--;
	}

	return 0;
}

/*
 * Delete a directory
 * (See the documentation for unlink if needed)
 */
int lfs_rmdir( const char *path ){
	int idx = get_dir_index(path, 0);
	int p_idx = get_dir_index(dir_list[idx].parent, 1);

	if (strcmp(dir_list[idx].parent, "home") == 0){
		int j = -1;
		for (int i = 0; i <= home.size; i++){
			path++;
			if (strcmp(home.files[i], path) == 0){
				j = i;
			}
			path--;
		}
		int i;
		for (i = j; i <= home.size; i++){
			strcpy(home.files[i], home.files[i+1]);
		}
		home.size--;

		for (i = idx; i < dir_list_size; i++){
			strcpy(dir_list[i].name, dir_list[i+1].name);
			strcpy(dir_list[i].parent, dir_list[i+1].parent);
			dir_list[i].size = dir_list[i+1].size;
			memmove(&(dir_list[i].files), &(dir_list[i+1].files),
				(dir_list_size-i-1)*sizeof*dir_list[i].files);
		}
		memset(&dir_list[i], 0, sizeof*dir_list);
		dir_list_size--;
	} else {
		int j = -1;
		for (int i = 0; i <= dir_list[p_idx].size; i++){
			path++;
			if (strcmp(dir_list[p_idx].files[i], path) == 0)
				j = i;
			path--;
		}
		
		int i;
		for (i = j; i <= dir_list[p_idx].size; i++){
			strcpy(dir_list[p_idx].files[i], dir_list[p_idx].files[i+1]);
		}
		dir_list[p_idx].size--;

		for (i = idx; i < dir_list_size; i++){
			strcpy(dir_list[i].name, dir_list[i+1].name);
			strcpy(dir_list[i].parent, dir_list[i+1].parent);
			dir_list[i].size = dir_list[i+1].size;
			memmove(&(dir_list[i].files), &(dir_list[i+1].files),
				(dir_list_size-i-1)*sizeof*dir_list[i].files);
		}
		memset(&dir_list[i], 0, sizeof*dir_list);
		dir_list_size--;
	}

	return 0;
}

/*
 * Open a file
 */
int lfs_open( const char *path, struct fuse_file_info *fi) {
	if ((fi->flags & 3) == O_WRONLY)
		return 0;

	else if ((fi->flags & 3) == O_RDONLY)
		return 0;

	return -EACCES;
}

/*
 * Close a file
 */
int lfs_release(const char *path, struct fuse_file_info *fi){
	if ((fi->flags & 3) == O_WRONLY)
		return 0;

	else if ((fi->flags & 3) == O_RDONLY)
		return 0;

	return -EACCES;
}

int main( int argc, char *argv[] ) {
    strcpy( home.name, "home" );
    strcpy(home.parent, "");
    home.size = -1;
    fuse_main( argc, argv, &lfs_oper );

    return 0;
}