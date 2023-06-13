package com.example.moyiza_be.user.service;

import com.example.moyiza_be.club.service.ClubService;
import com.example.moyiza_be.common.enums.BasicProfileEnum;
import com.example.moyiza_be.common.enums.TagEnum;
import com.example.moyiza_be.common.redis.RedisUtil;
import com.example.moyiza_be.common.security.jwt.CookieUtil;
import com.example.moyiza_be.common.security.jwt.JwtUtil;
import com.example.moyiza_be.common.security.jwt.refreshToken.RefreshTokenRepository;
import com.example.moyiza_be.common.utils.AwsS3Uploader;
import com.example.moyiza_be.user.dto.*;
import com.example.moyiza_be.user.entity.User;
import com.example.moyiza_be.user.repository.UserRepository;
import com.example.moyiza_be.user.sms.SmsUtil;
import com.example.moyiza_be.user.util.ValidationUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final CookieUtil cookieUtil;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AwsS3Uploader awsS3Uploader;
    private final ClubService clubService;
    private final SmsUtil smsUtil;
    private final ValidationUtil validationUtil;
    private final RedisUtil redisUtil;

    //Signup
    @Transactional
    public ResponseEntity<?> signup(SignupRequestDto requestDto, MultipartFile imageFile) {
        String password = passwordEncoder.encode(requestDto.getPassword());
        String storedFileUrl = BasicProfileEnum.getRandomImage().getImageUrl();
        checkDuplicatedEmail(requestDto.getEmail());
        checkDuplicatedNick(requestDto.getNickname());
        if(imageFile != null){
            storedFileUrl  = awsS3Uploader.uploadFile(imageFile);
        }
        User user = new User(password, requestDto, storedFileUrl);
        user.authorizeUser();
        userRepository.save(user);
        return new ResponseEntity<>("Sign up successfully", HttpStatus.OK);
    }

    @Transactional
    public ResponseEntity<?> updateSocialInfo(UpdateSocialInfoRequestDto requestDto, User user) {
        User foundUser = findUser(user.getEmail());
        checkDuplicatedNick(requestDto.getNickname());
        foundUser.updateSocialInfo(requestDto);
        foundUser.authorizeUser();
        return new ResponseEntity<>("Social signup complete!", HttpStatus.OK);
    }

    public ResponseEntity<?> getSocialInfo(User user) {
//        User foundUser = findUser(user.getEmail());
        SocialInfoResponseDto responseDto = new SocialInfoResponseDto(user.getName(), user.getNickname());
        return new ResponseEntity<>(responseDto, HttpStatus.OK);
    }

    //Login
    public ResponseEntity<?> login(LoginRequestDto requestDto, HttpServletResponse response) {
        String email = requestDto.getEmail();
        String password = requestDto.getPassword();
        User user = findUser(email);
        if(!passwordEncoder.matches(password, user.getPassword())){
            throw new IllegalArgumentException("Invalid password.");
        }
        jwtUtil.createAndSetToken(response, user);
        return new ResponseEntity<>("Successful login", HttpStatus.OK);
    }

    //Logout
    @Transactional
    public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response, String email) {
        cookieUtil.deleteCookie(request, response, "REFRESH_TOKEN");
        refreshTokenRepository.deleteByEmail(email).orElseThrow(
                ()-> new NoSuchElementException("You are not the logged in user."));
        return new ResponseEntity<>("Successful logout", HttpStatus.OK);
    }

    //Modify Profile
    @Transactional
    public ResponseEntity<?> updateProfile(MultipartFile imageFile, UpdateRequestDto requestDto, String email) {
        User user = findUser(email);
        checkDuplicatedNick(requestDto.getNickname());

        if(imageFile != null){
            awsS3Uploader.delete(user.getProfileImage());
            String storedFileUrl  = awsS3Uploader.uploadFile(imageFile);
            user.updateProfileImage(storedFileUrl);
        }

        List<TagEnum> tagEnumList = requestDto.getTagEnumList();
        String newString = "0".repeat(TagEnum.values().length);
        StringBuilder tagBuilder = new StringBuilder(newString);
        for (TagEnum tagEnum : tagEnumList) {
            tagBuilder.setCharAt(tagEnum.ordinal(), '1');
        }
        user.updateProfile(requestDto.getNickname(), tagBuilder.toString());

        return new ResponseEntity<>("Edit your membership information", HttpStatus.OK);
    }

    //Reissue Token
    @Transactional
    public ResponseEntity<?> reissueToken(String refreshToken, HttpServletResponse response) {
        jwtUtil.refreshTokenValid(refreshToken);
        String userEmail = jwtUtil.getUserInfoFromToken(refreshToken);
        User user = userRepository.findByEmail(userEmail).get();
        String newAccessToken = jwtUtil.createToken(user, "Access");
        response.setHeader("ACCESS_TOKEN", newAccessToken);
        return new ResponseEntity<>("Successful token reissue!", HttpStatus.OK);
    }

    //Check for email duplicates
    public ResponseEntity<?> isDuplicatedEmail(CheckEmailRequestDto requestDto) {
        checkDuplicatedEmail(requestDto.getEmail());
        Map<String, Boolean> result = new HashMap<>();
        result.put("isDuplicatedEmail", false);
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    //Check for nickname duplicates
    public ResponseEntity<?> isDuplicatedNick(CheckNickRequestDto requestDto) {
        checkDuplicatedNick(requestDto.getNickname());
        Map<String, Boolean> result = new HashMap<>();
        result.put("isDuplicatedNick", false);
        return new ResponseEntity<>(result, HttpStatus.OK);
    }
    //Find email - Send text
    @Transactional
    public ResponseEntity<?> sendSmsToFindEmail(FindEmailRequestDto requestDto) {
        String name = requestDto.getName();
        String phoneNum = requestDto.getPhone().replaceAll("-","");
        User foundUser = userRepository.findByNameAndPhone(name, phoneNum).orElseThrow(()->
                new NoSuchElementException("The user does not exist."));
        String receiverEmail = foundUser.getEmail();
        String verificationCode = validationUtil.createCode();
        smsUtil.sendSms(phoneNum, verificationCode);
        redisUtil.setDataExpire(verificationCode, receiverEmail, 60 * 5L); //Valid for 5 minutes

        return new ResponseEntity<>("Text sent successfully", HttpStatus.OK);
    }

    public ResponseEntity<?> verifyCodeToFindEmail(String code) {
        String userEmail = redisUtil.getData(code);
        if(userEmail == null){
            throw new NullPointerException("Invalid authentication number.");
        }
        redisUtil.deleteData(code);
        EmailResponseDto responseDto = new EmailResponseDto(userEmail);
        return new ResponseEntity<>(responseDto, HttpStatus.OK);
    }

    //Test
    public ResponseEntity<?> uploadTest(MultipartFile image) {
        if(image.isEmpty()){
            return new ResponseEntity<>(BasicProfileEnum.getRandomImage().getImageUrl(), HttpStatus.OK);
        }
        String storedFileUrl  = awsS3Uploader.uploadFile(image);
        return new ResponseEntity<>(storedFileUrl, HttpStatus.OK);
    }

    @Transactional
    public ResponseEntity<?> signupTest(TestSignupRequestDto testRequestDto) {
        String password = passwordEncoder.encode(testRequestDto.getPassword());
        checkDuplicatedEmail(testRequestDto.getEmail());
        checkDuplicatedNick(testRequestDto.getNickname());
        User user = new User(password, testRequestDto);
        user.authorizeUser();
        userRepository.save(user);
        return new ResponseEntity<>("🎊테스트 성공!!🎊 고생하셨어요ㅠㅠ", HttpStatus.OK);
    }

    public User findUser(String email){
        return userRepository.findByEmail(email).orElseThrow(()->
                new NoSuchElementException("The user does not exist."));
    }

    public void checkDuplicatedEmail(String email){
        Optional<User> findUserByEmail = userRepository.findByEmail(email);
        if (findUserByEmail.isPresent()) {
            throw new IllegalArgumentException("Using duplicate emails");
        }
    }
    public void checkDuplicatedNick(String nickname){
        Optional<User> findUserByNickname = userRepository.findByNickname(nickname);
        if (findUserByNickname.isPresent()) {
            throw new IllegalArgumentException("Using duplicate nickname");
        }
    }

    public List<User> loadUserListByIdList(List<Long> userIdList){    // Used to view club members
        return userRepository.findAllById(userIdList);
    }

    public User loadUserById(Long userId){
        return userRepository.findById(userId).orElseThrow(
                () -> new NullPointerException("User not found"));
    }
}
