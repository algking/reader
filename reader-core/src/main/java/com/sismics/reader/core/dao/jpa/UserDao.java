package com.sismics.reader.core.dao.jpa;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Query;

import org.mindrot.jbcrypt.BCrypt;

import com.google.common.base.Joiner;
import com.sismics.reader.core.dao.jpa.dto.UserDto;
import com.sismics.reader.core.model.jpa.User;
import com.sismics.reader.core.util.jpa.PaginatedList;
import com.sismics.reader.core.util.jpa.PaginatedLists;
import com.sismics.reader.core.util.jpa.SortCriteria;
import com.sismics.util.context.ThreadLocalContext;

/**
 * User DAO.
 * 
 * @author jtremeaux
 */
public class UserDao {
    /**
     * Authenticates an user.
     * 
     * @param username User login
     * @param password User password
     * @return ID of the authenticated user or null
     */
    public String authenticate(String username, String password) {
        EntityManager em = ThreadLocalContext.get().getEntityManager();
        Query q = em.createQuery("select u from User u where u.username = :username and u.deleteDate is null");
        q.setParameter("username", username);
        try {
            User user = (User) q.getSingleResult();
            user.setLastLoginDate(new Date());
            if (!BCrypt.checkpw(password, user.getPassword())) {
                return null;
            }
            return user.getId();
        } catch (NoResultException e) {
            return null;
        }
    }
    
    /**
     * Creates a new user.
     * 
     * @param user User to create
     * @return User ID
     * @throws Exception
     */
    public String create(User user) throws Exception {
        // Create the user UUID
        user.setId(UUID.randomUUID().toString());
        
        // Checks for user unicity
        EntityManager em = ThreadLocalContext.get().getEntityManager();
        Query q = em.createQuery("select u from User u where u.username = :username and u.deleteDate is null");
        q.setParameter("username", user.getUsername());
        List<?> l = q.getResultList();
        if (l.size() > 0) {
            throw new Exception("AlreadyExistingUsername");
        }
        
        user.setCreateDate(new Date());
        user.setPassword(hashPassword(user.getPassword()));
        em.persist(user);
        
        return user.getId();
    }
    
    /**
     * Updates a user.
     * 
     * @param user User to update
     * @return Updated user
     */
    public User update(User user) {
        EntityManager em = ThreadLocalContext.get().getEntityManager();
        
        // Get the user
        Query q = em.createQuery("select u from User u where u.id = :id and u.deleteDate is null");
        q.setParameter("id", user.getId());
        User userFromDb = (User) q.getSingleResult();

        // Update the user
        userFromDb.setLocaleId(user.getLocaleId());
        userFromDb.setEmail(user.getEmail());
        userFromDb.setDisplayTitleWeb(user.isDisplayTitleWeb());
        userFromDb.setDisplayTitleMobile(user.isDisplayTitleMobile());
        userFromDb.setDisplayUnreadWeb(user.isDisplayUnreadWeb());
        userFromDb.setDisplayUnreadMobile(user.isDisplayUnreadMobile());
        userFromDb.setFirstConnection(user.isFirstConnection());
        
        return user;
    }
    
    /**
     * Update the user password.
     * 
     * @param user User to update
     * @return Updated user
     */
    public User updatePassword(User user) {
        EntityManager em = ThreadLocalContext.get().getEntityManager();
        
        // Get the user
        Query q = em.createQuery("select u from User u where u.id = :id and u.deleteDate is null");
        q.setParameter("id", user.getId());
        User userFromDb = (User) q.getSingleResult();

        // Update the user
        userFromDb.setPassword(hashPassword(user.getPassword()));
        
        return user;
    }

    /**
     * Gets a user by its ID.
     * 
     * @param id User ID
     * @return User
     */
    public User getById(String id) {
        EntityManager em = ThreadLocalContext.get().getEntityManager();
        try {
            return em.find(User.class, id);
        } catch (NoResultException e) {
            return null;
        }
    }
    
    /**
     * Gets an active user by its username.
     * 
     * @param username User's username
     * @return User
     */
    public User getActiveByUsername(String username) {
        EntityManager em = ThreadLocalContext.get().getEntityManager();
        try {
            Query q = em.createQuery("select u from User u where u.username = :username and u.deleteDate is null");
            q.setParameter("username", username);
            return (User) q.getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }
    
    /**
     * Gets an active user by its password recovery token.
     * 
     * @param passwordResetKey Password recovery token
     * @return User
     */
    public User getActiveByPasswordResetKey(String passwordResetKey) {
        EntityManager em = ThreadLocalContext.get().getEntityManager();
        try {
            Query q = em.createQuery("select u from User u where u.passwordResetKey = :passwordResetKey and u.deleteDate is null");
            q.setParameter("passwordResetKey", passwordResetKey);
            return (User) q.getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }
    
    /**
     * Deletes a user.
     * 
     * @param username User's username
     */
    public void delete(String username) {
        EntityManager em = ThreadLocalContext.get().getEntityManager();
            
        // Get the user
        Query q = em.createQuery("select u from User u where u.username = :username and u.deleteDate is null");
        q.setParameter("username", username);
        User userFromDb = (User) q.getSingleResult();

        // Delete the user
        userFromDb.setDeleteDate(new Date());
    }

    /**
     * Hash the user's password.
     * 
     * @param password Clear password
     * @return Hashed password
     */
    protected String hashPassword(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt());
    }
    
    /**
     * Returns the list of all users.
     * 
     * @param paginatedList List of users (updated by side effects)
     * @param sortCriteria Sort criteria
     */
    public void findAll(PaginatedList<UserDto> paginatedList, SortCriteria sortCriteria) {
        Map<String, Object> parameterMap = new HashMap<String, Object>();
        StringBuilder sb = new StringBuilder("select u.USE_ID_C as c0, u.USE_USERNAME_C as c1, u.USE_EMAIL_C as c2, u.USE_CREATEDATE_D as c3, u.USE_LASTLOGINDATE_D as c4, u.USE_IDLOCALE_C as c5");
        sb.append(" from T_USER u ");
        
        // Add search criterias
        List<String> criteriaList = new ArrayList<String>();
        criteriaList.add("u.USE_DELETEDATE_D is null");
        
        if (!criteriaList.isEmpty()) {
            sb.append(" where ");
            sb.append(Joiner.on(" and ").join(criteriaList));
        }
        
        // Perform the search
        List<Object[]> l = PaginatedLists.executePaginatedQuery(paginatedList, sb.toString(), parameterMap, sortCriteria);
        
        // Assemble results
        List<UserDto> userDtoList = new ArrayList<UserDto>();
        for (Object[] o : l) {
            int i = 0;
            UserDto userDto = new UserDto();
            userDto.setId((String) o[i++]);
            userDto.setUsername((String) o[i++]);
            userDto.setEmail((String) o[i++]);
            userDto.setCreateTimestamp(((Timestamp) o[i++]).getTime());
            Timestamp lastLoginTimestamp = ((Timestamp) o[i++]);
            if (lastLoginTimestamp != null) {
                userDto.setLastLoginTimestamp(lastLoginTimestamp.getTime());
            }
            userDto.setLocaleId((String) o[i++]);
            userDtoList.add(userDto);
        }
        paginatedList.setResultList(userDtoList);
    }
}
