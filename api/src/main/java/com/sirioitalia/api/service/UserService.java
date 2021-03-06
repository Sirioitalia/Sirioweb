package com.sirioitalia.api.service;

import com.sirioitalia.api.exception.ResourceException;
import com.sirioitalia.api.model.User;
import com.sirioitalia.api.projection.UserProjection;
import com.sirioitalia.api.repository.UserRepository;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.projection.SpelAwareProxyProjectionFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;


@Service
@Data
public class UserService implements UserDetailsService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ProjectionFactory projectionFactory = new SpelAwareProxyProjectionFactory();

    @Autowired
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
    }


    public UserProjection.Full getUserById(Long itemId) throws ResourceException {
        UserProjection.Full user = userRepository.findProjectionById(itemId)
                .orElseThrow(() -> new ResourceException("FindUserFailed", HttpStatus.NOT_FOUND.getReasonPhrase(), HttpStatus.NOT_FOUND));

        return user;
    }

    public UserProjection.Authentication getUserByEmail(String email) throws ResourceException {
        UserProjection.Authentication user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceException("FindUserFailed", HttpStatus.NOT_FOUND.getReasonPhrase(), HttpStatus.NOT_FOUND));

        return user;
    }

    public Iterable<UserProjection.Full> getUsers() {
        return userRepository.findBy();
    }

    @Transactional
    public void deleteUser(Long userId) {
        User userToDelete = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceException(HttpStatus.NOT_FOUND.getReasonPhrase(), "User not found"));


        userRepository.delete(userToDelete);
    }

    @Transactional
    public UserProjection.Full updateUser(Long userId, User userDetails) throws ResourceException {
        try {
            User foundedUser = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceException(HttpStatus.NOT_FOUND.getReasonPhrase(), "User not found"));


            foundedUser.setEmail(userDetails.getEmail() == null
                    ? foundedUser.getEmail()
                    : userDetails.getEmail());

            foundedUser.setFirstName(userDetails.getFirstName() == null
                    ? foundedUser.getFirstName()
                    : userDetails.getFirstName());

            foundedUser.setLastName(userDetails.getLastName() == null
                    ? foundedUser.getLastName()
                    : userDetails.getLastName());

            if (userDetails.getPasswordHash() != null) {
                String formattedPassword = String.format("%s:%s", foundedUser.getPasswordHash(), foundedUser.getPasswordSalt());

                if (!passwordEncoder.matches(userDetails.getPasswordHash(), formattedPassword)) {
                    HashMap<String, String> hashedPassword = encodePassword(userDetails.getPasswordHash());
                    foundedUser.setPasswordHash(hashedPassword.get("hash"));
                    foundedUser.setPasswordSalt(hashedPassword.get("salt"));
                }
            }

            if (userDetails.getAddress() != null) {
                foundedUser.getAddress().setCity(userDetails.getAddress().getCity() == null
                        ? foundedUser.getAddress().getCity()
                        : userDetails.getAddress().getCity());

                foundedUser.getAddress().setStreetName(userDetails.getAddress().getStreetName() == null
                        ? foundedUser.getAddress().getStreetName()
                        : userDetails.getAddress().getStreetName());

                foundedUser.getAddress().setStreetNumber(userDetails.getAddress().getStreetNumber() == null
                        ? foundedUser.getAddress().getStreetNumber()
                        : userDetails.getAddress().getStreetNumber());

                foundedUser.getAddress().setZipCode(userDetails.getAddress().getZipCode() == null
                        ? foundedUser.getAddress().getZipCode()
                        : userDetails.getAddress().getZipCode());

            }

            foundedUser.setPhoneNumber(userDetails.getPhoneNumber() == null
                    ? foundedUser.getPhoneNumber()
                    : userDetails.getPhoneNumber());

            User updatedUser = userRepository.save(foundedUser);


            return projectionFactory.createProjection(UserProjection.Full.class, updatedUser);
        } catch (Exception e) {
            throw new ResourceException(e.getMessage(), e.getCause(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Transactional
    public User createUser(User userDetails) throws ResourceException {
        try {
            HashMap<String, String> hashedPassword = encodePassword(userDetails.getPasswordHash());

            userDetails.setPasswordHash(hashedPassword.get("hash"));
            userDetails.setPasswordSalt(hashedPassword.get("salt"));


            return userRepository.save(userDetails);
        } catch (Exception e) {
            throw new ResourceException(e.getMessage(), e.getCause(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private HashMap<String, String> encodePassword(CharSequence password) {
        HashMap<String, String> hashedPassword = new HashMap<>();

        String[] hashAndSaltPassword = passwordEncoder.encode(password).split(":");

        hashedPassword.put("hash", hashAndSaltPassword[0]);
        hashedPassword.put("salt", hashAndSaltPassword[1]);


        return hashedPassword;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        UserProjection.Authentication foundedUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("No email founded for this user"));

        Collection<SimpleGrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority(foundedUser.getRoleLabel()));

        String formattedPassword = String.format("%s:%s", foundedUser.getPasswordHash(), foundedUser.getPasswordSalt());

        return new org.springframework.security.core.userdetails.User(foundedUser.getEmail(),
                formattedPassword, authorities);
    }
}