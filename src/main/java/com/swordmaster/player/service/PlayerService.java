package com.swordmaster.player.service;

import com.swordmaster.common.BusinessException;
import com.swordmaster.equipment.sword.table.SwordTable;
import com.swordmaster.player.entity.Player;
import com.swordmaster.player.repository.PlayerRepository;
import com.swordmaster.user.entity.User;
import com.swordmaster.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlayerService {
    private final PlayerRepository playerRepository;
    private final UserRepository   userRepository;
    private final SwordTable       swordTable;

    // 조회
    public Player getMe(Long userId) {
        return playerRepository.findByUser_Id(userId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "플레이어 정보가 없습니다."));
    }

    // 생성
    @Transactional
    public Player create(Long userId) {
        // 비정상 접근에 대한 예외 처리
        if (playerRepository.existsByUser_Id(userId)) {
            throw new BusinessException(HttpStatus.CONFLICT, "플레이어가 이미 존재합니다.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "유저 정보가 없습니다."));

        // 같은 요청이 동시에 들어올 경우에 대한 Unique 처리
        try {
            // Flush를 통해 트랜잭션이 끝날 때 까지 미루지 않고 바로 INSERT 요청
            return playerRepository.saveAndFlush(new Player(user, swordTable.getFirst().level()));
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(HttpStatus.CONFLICT, "플레이어가 이미 존재합니다.");
        }
    }
}
