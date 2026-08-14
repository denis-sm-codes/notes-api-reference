package exception;

import entity.User;

public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(String name){
        super("User " + name + " not found");
    }
    public UserNotFoundException(Long id){
        super("User with id: " + id + " not found");
    }
}
