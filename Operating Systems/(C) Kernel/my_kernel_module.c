#ifndef __KERNEL__
#  define __KERNEL__
#endif
#ifndef MODULE
#  define MODULE
#endif

#include <linux/cdev.h>
#include <linux/module.h>
#include <linux/init.h>
#include <linux/slab.h>
#include <linux/kernel.h>
#include <linux/fs.h>
#include <linux/errno.h>
#include <linux/types.h>
#include <linux/wait.h>
/* #include <asm/uaccess.h> */
#include <linux/uaccess.h>
#include <linux/semaphore.h>
/* #include <asm/system.h> */
#include <asm/switch_to.h>
/* Prototypes - this would normally go in a .h file */
static int dm510_open( struct inode*, struct file* );
static int dm510_release( struct inode*, struct file* );
static ssize_t dm510_read( struct file*, char*, size_t, loff_t* );
static ssize_t dm510_write( struct file*, const char*, size_t, loff_t* );
long dm510_ioctl(struct file *filp, unsigned int cmd, unsigned long arg);

#define DEVICE_NAME "dm510_dev" /* Dev name as it appears in /proc/devices */
#define MAJOR_NUMBER 254
#define MIN_MINOR_NUMBER 0
#define MAX_MINOR_NUMBER 1

#define DM510_IOC_MAGIC 'k'
#define DM510_IOCSBUFFER _IOWR(DM510_IOC_MAGIC, 1, int)
#define DM510_IOCSMAXPROC _IOWR(DM510_IOC_MAGIC, 2, int)
#define DM510_IOC_MAXNR 2

#define DEVICE_COUNT 2
/* end of what really should have been in a .h file */

/* file operations struct */
static struct file_operations dm510_fops = {
	.owner   = THIS_MODULE,
	.read    = dm510_read,
	.write   = dm510_write,
	.open    = dm510_open,
	.release = dm510_release,
	.unlocked_ioctl   = dm510_ioctl
};

/* device structure */
struct dm510_dev {
	wait_queue_head_t inq, outq;       /* read and write queues */
	int buffersize;                    /* used in pointer arithmetic */
	int nreaders, nwriters;            /* number of openings for r/w in device0 */
	struct mutex mutex;               /* mutual exclusion semaphore 0*/
	struct cdev cdev;                  /* Char device structure */
};

int dm510_dev_buffer = 1024;    /* buffer size */
int dm510_dev_processes = 2;    /* maximum number of read processes */

/*bounded buffers and their ends*/
char *boundedbuffer0, *end0;
char *boundedbuffer1, *end1;
/* pointers to where to write and read from*/
char *rp0, *rp1, *wp0, *wp1;

static struct dm510_dev *dm510_dev_devices; /* the given device */
dev_t dm510_dev_devno;  /* Our first device number */

/*
 * Set up a cdev entry.
 */
static void dm510_setup_cdev(struct dm510_dev *dev, int index)
{
	int err, devno = dm510_dev_devno + index;

	cdev_init(&dev->cdev, &dm510_fops);
	dev->cdev.owner = THIS_MODULE;
	err = cdev_add(&dev->cdev, devno, 1);
	/* Fail gracefully if need be*/
	if (err)
		printk(KERN_NOTICE "Error %d adding dm510_dev %d", err, index);
}

/* called when module is loaded */
int dm510_init_module( void ) {

	/* initialization code belongs here */
	int i, result;
	dev_t dev = 0;
	dev = MKDEV(MAJOR_NUMBER, MIN_MINOR_NUMBER);

	/* allocate major number */
	result = register_chrdev_region(dev, DEVICE_COUNT, DEVICE_NAME);
	if (result < 0) {
		printk(KERN_WARNING "Can't get major %d\n", MAJOR_NUMBER);
		return result;
	}
	dm510_dev_devno = dev;

	/* allocate memory for device */
	dm510_dev_devices = kmalloc(DEVICE_COUNT * sizeof(struct dm510_dev), GFP_KERNEL);
	if (dm510_dev_devices == NULL) {
		unregister_chrdev_region(dev, DEVICE_COUNT);
		return 0;
	}
	memset(dm510_dev_devices, 0, DEVICE_COUNT * sizeof(struct dm510_dev));

	for (i = 0; i < DEVICE_COUNT; i++) {
		/* initialize semaphores and waiting queues*/
		init_waitqueue_head(&(dm510_dev_devices[i].inq));
		init_waitqueue_head(&(dm510_dev_devices[i].outq));
		mutex_init(&dm510_dev_devices[i].mutex);
		dm510_setup_cdev(dm510_dev_devices + i, i);
	}

	printk(KERN_INFO "DM510: Hello from your device!\n");
	return 0;
}

/* Called when module is unloaded */
void dm510_cleanup_module( void ) {

	/* clean up code belongs here */
	int i;

	if (!dm510_dev_devices)
		return; /* nothing else to release */

	/*free the memory delete cdevs*/
	for (i = 0; i < DEVICE_COUNT; i++) {
		cdev_del(&dm510_dev_devices[i].cdev);
	}
	/* free the buffers and the device*/
	kfree(boundedbuffer0);
	kfree(boundedbuffer1);
	boundedbuffer0 = NULL;
	boundedbuffer1 = NULL;
	kfree(dm510_dev_devices);

	/* free device numbers */
	unregister_chrdev(MKDEV(MAJOR_NUMBER, MIN_MINOR_NUMBER), DEVICE_NAME);
	dm510_dev_devices = NULL;
	printk(KERN_INFO "DM510: Module unloaded.\n");
}

/* Called when a process tries to open the device file */
static int dm510_open( struct inode *inode, struct file *filp ) {

	/* device claiming code belongs here */

	/* device information*/
	struct dm510_dev *dev;
	/* find appropriate device structure */
	dev = container_of(inode->i_cdev, struct dm510_dev, cdev);
	filp->private_data = dev;

	/*  make sure that no accesses to the data structure are made without holding the semaphore */
	if (mutex_lock_interruptible(&dev->mutex))
		return -ERESTARTSYS;

	if (!boundedbuffer0) {
		/* allocate the first buffer */
		boundedbuffer0 = kmalloc(dm510_dev_buffer, GFP_KERNEL);
		if (!boundedbuffer0) {
			mutex_unlock(&dev->mutex);
			return -ENOMEM;
		}
		dev->buffersize = dm510_dev_buffer;
			end0 = boundedbuffer0 + dev->buffersize;
			rp0 = wp0 = boundedbuffer0;
	}
	if (!boundedbuffer1){
		/* allocate the second buffer */
		boundedbuffer1 = kmalloc(dm510_dev_buffer, GFP_KERNEL);
		if (!boundedbuffer1){
			mutex_unlock(&dev->mutex);
			return -ENOMEM;
		}
		dev->buffersize = dm510_dev_buffer;
		end1 = boundedbuffer1 + dev->buffersize;
		rp1 = wp1 = boundedbuffer1;
	}
	dev->buffersize = dm510_dev_buffer;

	/* increase number of readers */
	if (filp->f_mode & FMODE_READ){
		/* there are too may readers */
		if(dev->nreaders >= dm510_dev_processes){
			printk(KERN_NOTICE "Too many readers\n");
			mutex_unlock(&dev->mutex);
			return -EACCES;
		}
		dev->nreaders++;
	}
	/* increse number of writers */
	if (filp->f_mode & FMODE_WRITE){
		/* there already is a writer */
		if(dev->nwriters){
			printk(KERN_NOTICE "Too many writers\n");
			mutex_unlock(&dev->mutex);
			return -EACCES;
		}
		dev->nwriters++;
	}
	mutex_unlock(&dev->mutex);

	return nonseekable_open(inode, filp);
}

/* Called when a process closes the device file. */
static int dm510_release( struct inode *inode, struct file *filp ) {

	/* device release code belongs here */
	struct dm510_dev *dev = filp->private_data;

	mutex_lock(&dev->mutex);
	/* decrease number of readers/writer*/
	if (filp->f_mode & FMODE_READ)
		dev->nreaders--;
	if (filp->f_mode & FMODE_WRITE)
		dev->nwriters--;
	if (dev->nwriters + dev->nreaders == 0){
		/* free the buffers and the device*/
		kfree(boundedbuffer0);
		kfree(boundedbuffer1);
		boundedbuffer0 = NULL;
		boundedbuffer1 = NULL;
	}
	mutex_unlock(&dev->mutex);

	return 0;
}

/* Called when a process, which already opened the dev file, attempts to read from it. */
static ssize_t dm510_read( struct file *filp,
	char *buf,      /* The buffer to fill with data     */
	size_t count,   /* The max number of bytes to read  */
	loff_t *f_pos )  /* The offset in the file          */
{
	/* read code belongs here */
	struct dm510_dev *dev = filp->private_data;

	/* this is device 1 */
	if(dev->cdev.dev == (dm510_dev_devices + 1)->cdev.dev){
		
		if (mutex_lock_interruptible(&dev->mutex))
			return -ERESTARTSYS;

		/* nothing to read */
		while (rp1 == wp1){
			/* release the lock */
			mutex_unlock(&dev->mutex);
			if(filp->f_flags & O_NONBLOCK){
				return -EAGAIN;
			}
			/* Since there is nothing to read, hibernate and wait to be awaken */
			if (wait_event_interruptible(dev->inq, (rp1 != wp1))){
				return -ERESTARTSYS;
			}
			if (mutex_lock_interruptible(&dev->mutex))
			return -ERESTARTSYS;
		}

		/* data is there */
		/* get the length of the data */
		if (wp1 > rp1)
			count = min(count, (size_t)(wp1 - rp1));
		else
			count = min(count, (size_t)(end1 - rp1));
			/* and copy the data to user */
		if (copy_to_user(buf, rp1, count)) {
			mutex_unlock(&dev->mutex);
			return -EFAULT;
		}
		rp1 += count;
		if (rp1 == end1)
			rp1 = boundedbuffer1;
		mutex_unlock (&dev->mutex);
		
		/* wake up any writers*/
		wake_up_interruptible(&dm510_dev_devices[0].outq);

	/* this is device 0*/
	} else if (dev->cdev.dev == (dm510_dev_devices + 0)->cdev.dev){

		if (mutex_lock_interruptible(&dev->mutex))
			return -ERESTARTSYS;

		/* nothing to read */
		while (rp0 == wp0){
			/* release the lock */
			mutex_unlock(&dev->mutex);
			if(filp->f_flags & O_NONBLOCK){
				return -EAGAIN;
			}
			/* Since there is nothing to read, hibernate and wait to be awaken */
			if (wait_event_interruptible(dev->inq, (rp0 != wp0))){
				return -ERESTARTSYS;
			}
			if (mutex_lock_interruptible(&dev->mutex))
				return -ERESTARTSYS;
		}

		/* data is there */
		/* get the length of the data */
		if (wp0 > rp0)
			count = min(count, (size_t)(wp0 - rp0));
		else
			count = min(count, (size_t)(end0 - rp0));
		/* and copy the data to user */
		if (copy_to_user(buf, rp0, count)) {
			mutex_unlock(&dev->mutex);
			return -EFAULT;
		}
		rp0 += count;
		if (rp0 == end0)
			rp0 = boundedbuffer0;
		mutex_unlock (&dev->mutex);

		/* wake up any writers*/
		wake_up_interruptible(&dm510_dev_devices[1].outq);
	}
	return count; /* return number of bytes read */
}

/* How much space is free? */
static int spacefree(struct dm510_dev *dev, int devicenr)
{
	if (devicenr == 0){
		if (rp1 == wp1)
			return dev->buffersize - 1;
		return ((rp1 + dev->buffersize - wp1) % dev->buffersize) - 1;
	} else {
		if (rp0 == wp0)
			return dev->buffersize -1;
		return ((rp0 + dev->buffersize - wp0) % dev->buffersize) - 1;
	}
}

/* Wait for space for writing; caller must hold device semaphore.  On
 * error the semaphore will be released before returning. */
static int dm510_getwritespace(struct dm510_dev *dev, struct file *filp, int devicenr)
{
	while (spacefree(dev, devicenr) == 0) { /* full */
		DEFINE_WAIT(wait);

		mutex_unlock(&dev->mutex);
		if (filp->f_flags & O_NONBLOCK)
			return -EAGAIN;
		prepare_to_wait(&dev->outq, &wait, TASK_INTERRUPTIBLE);
		if (spacefree(dev, devicenr) == 0)
			schedule();
		finish_wait(&dev->outq, &wait);
		if (signal_pending(current))
			return -ERESTARTSYS; /* signal: tell the fs layer to handle it */
		if (mutex_lock_interruptible(&dev->mutex))
			return -ERESTARTSYS;
	}
	return 0;
}

/* Called when a process writes to dev file */
static ssize_t dm510_write( struct file *filp,
        const char *buf,/* The buffer to get data from      */
        size_t count,   /* The max number of bytes to write */
        loff_t *f_pos )  /* The offset in the file           */
{
	struct dm510_dev *dev = filp->private_data;
	int result;

	if (mutex_lock_interruptible(&dev->mutex))
		return -ERESTARTSYS;

	/* this is device 0*/
	if (dev->cdev.dev == (dm510_dev_devices + 0)->cdev.dev){

		/* make sure there is space to write */
		result = dm510_getwritespace(dev, filp, 0);
		if (result)
			return result;

		/* ok, space is there, accept something */
		count = min(count, (size_t)spacefree(dev, 0));
		/* get lenght of the data written*/
		if (wp1 >= rp1)
			count = min(count, (size_t)(end1 - wp1));
		else
			count = min(count, (size_t)(rp1 - wp1 - 1));
		/* write the data */
		if (copy_from_user(wp1, buf, count)) {
			mutex_unlock(&dev->mutex);
			return -EFAULT;
		}
		wp1 += count;
		if (wp1 == end1)
			wp1 = boundedbuffer1; /* wrapped */
		mutex_unlock(&dev->mutex);

		/* wake up any reader */
		wake_up_interruptible(&dm510_dev_devices[1].inq);
	
	/* this si device 1*/
	} else if (dev->cdev.dev == (dm510_dev_devices + 1)->cdev.dev) {
		/* make sure there is space to write */
		result = dm510_getwritespace(dev, filp, 1);
		if (result)
			return result;

		/* ok, space is there, accept something */
		count = min(count, (size_t)spacefree(dev, 1));
		/* get lenght of the data written*/
		if (wp0 >= rp0)
			count = min(count, (size_t)(end0 - wp0));
		else
			count = min(count, (size_t)(rp0 - wp0 - 1));
		/* write the data */
		if (copy_from_user(wp0, buf, count)) {
			mutex_unlock(&dev->mutex);
			return -EFAULT;
		}
		wp0 += count;
		if (wp0 == end0)
			wp0 = boundedbuffer0; /* wrapped */
		mutex_unlock(&dev->mutex);
		/* wake up any reader */
		wake_up_interruptible(&dm510_dev_devices[0].inq);
	}
	return count; /* return number of bytes written */
}

/* called by system call icotl */
long dm510_ioctl(
        struct file *filp,
        unsigned int cmd,   /* command passed from the user */
        unsigned long arg ) /* argument of the command */
{
	/* ioctl code belongs here */
	printk(KERN_INFO "DM510: ioctl called.\n");

	/* if not magic number or if not valid command*/
	if (_IOC_TYPE(cmd) != DM510_IOC_MAGIC) return -ENOTTY;
	if (_IOC_NR(cmd) > DM510_IOC_MAXNR) return -ENOTTY;

	switch(cmd) {
		case DM510_IOCSBUFFER: /* set new buffer size */
			dm510_dev_buffer = arg;
			printk(KERN_INFO "New buffer size set to %d\n", dm510_dev_buffer);
			break;

		case DM510_IOCSMAXPROC: /* set new maximum number of processes*/
			dm510_dev_processes = arg;
			printk(KERN_INFO "New maximum number of processes set to %d\n", dm510_dev_processes);
			break;
	}

	return 0;
}

module_init( dm510_init_module );
module_exit( dm510_cleanup_module );

MODULE_AUTHOR( "...Remus Stefan Cernat" );
MODULE_LICENSE( "GPL" );