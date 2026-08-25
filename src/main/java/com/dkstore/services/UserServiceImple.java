package com.dkstore.services;



import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ansi.AnsiOutput.Enabled;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.dkstore.models.Product;
import com.dkstore.models.User;
import com.dkstore.repository.UserRepository;

@Service
public class UserServiceImple implements UserService{
	@Autowired
	private UserRepository userRepository;
	@Override
	public User findByUserName(String username) {
	    return userRepository.findByUserName(username);
	}
	@Override
	public List<User> getAll() {
		// TODO Auto-generated method stub
		return this.userRepository.findAll();
	}
	@Override
	public Boolean create(User user) {
		// TODO Auto-generated method stub
		if(userRepository.existsByUserName(user.getUserName())) {
			return false;
		}
		try {
	        if (user.getEnabled() == null) {
	            user.setEnabled(true);
	        }
			this.userRepository.save(user);
			return true;
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		return false;
	}
	@Override
	public Boolean update(User user) {
		// TODO Auto-generated method stub
		try {
			this.userRepository.save(user);
			return true;
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		return false;
	}
	@Override
	public Boolean delete(String userName) {
		// TODO Auto-generated method stub
		try {
			this.userRepository.delete(findByUserName(userName));
			;
			return true;
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		return false;
	}
	@Override
	public Page<User> getAll(Integer pageNo) {
		// TODO Auto-generated method stub
		Pageable pageable = PageRequest.of(Math.max(pageNo - 1, 0), 5);
		return this.userRepository.findAll(pageable);
	}
	@Override
	public List<User> search(String keyword) {
		// TODO Auto-generated method stub
		return this.userRepository.search(keyword);
	}
	@Override
	public Page<User> search(String keyword, Integer pageNo) {
		// TODO Auto-generated method stub
		List<User> list = this.search(keyword);
		Pageable pageable = PageRequest.of(Math.max(pageNo - 1, 0), 5);
		Integer start = (int) pageable.getOffset();
		Integer end = (int) ((pageable.getOffset() + pageable.getPageSize()) > list.size() ? list.size()
				: pageable.getOffset() + pageable.getPageSize());
		list = list.subList(start, end);
		return new PageImpl<User>(list, pageable, this.search(keyword).size());
	}
	@Override
	public Optional<User> findById(Integer id) {
		 return userRepository.findById(id);
	}
}
