#include "linux/kernel.h"
#include "linux/unistd.h"
#include "linux/slab.h"
#include "linux/uaccess.h"

typedef struct _msg_t msg_t;

struct _msg_t{
        msg_t* previous;
        int length;
        char* message;
};

static msg_t *bottom = NULL;
static msg_t *top = NULL;

asmlinkage
int sys_dm510_msgbox_put( char* buffer, int length ) {
        unsigned long flags;
        local_irq_save(flags);
        msg_t* msg = kmalloc(sizeof(msg_t), GFP_KERNEL);
        if (!msg){
                local_irq_restore(flags);
                return -ENOMEM;
        }
        msg->previous = NULL;
        msg->length = length;
        msg->message = kmalloc(length, GFP_KERNEL);
        if (!(msg -> message)){
                local_irq_restore(flags);
                return -ENOMEM;
        }
        if (access_ok(buffer, length)){
                copy_from_user(msg->message, buffer, length);
        } else {
                local_irq_restore(flags);
                return -EACCES;
        }
        if (bottom == NULL) {
                bottom = msg;
                top = msg;
        } else {
                /* not empty stack */
                msg->previous = top;
                top = msg;
        }
        local_irq_restore(flags);
        return 0;
}

asmlinkage
int sys_dm510_msgbox_get( char* buffer, int length ) {
        if (top != NULL) {
                unsigned long flags;
                local_irq_save(flags);
                msg_t* msg = top;
                int mlength = msg->length;
                top = msg->previous;
                if (access_ok(buffer, length)){
                        /* copy message */
                        copy_to_user(buffer, msg->message, mlength);
                        /* free memory */
                        kfree(msg->message);
                        kfree(msg);
                        local_irq_restore(flags);
                        return mlength;
                } else {
                        local_irq_restore(flags);
                        return -EACCES;
				}
		}
        return -1;
}