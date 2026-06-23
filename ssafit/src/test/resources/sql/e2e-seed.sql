INSERT INTO members (
    login_id,
    password_hash,
    email,
    nickname,
    status
) VALUES
    (
        'e2e_host',
        'pbkdf2_sha256$210000$YrkwRlujBFDdMgZbhzTRPw==$AF7GbWwvORg9FXpuct/hqK1n0F8NxusFdU2fZ1YG5ik=',
        'e2e.host@example.test',
        'E2E 주최자',
        'ACTIVE'
    ),
    (
        'e2e_participant',
        'pbkdf2_sha256$210000$H6Ec3iAkSjthfjr8MdcVKQ==$ZOnN7BnMhfOCaFOb3Iwk1sw2m0R3wJ3c9aSgdwy4WG0=',
        'e2e.participant@example.test',
        'E2E 참가자',
        'ACTIVE'
    ),
    (
        'e2e_full_member',
        'pbkdf2_sha256$210000$k+iNmkwkY2bz3qFMF9Z/mQ==$oOAf+rh2IRhmYDzxypKAy4KdJAe8mpNEJR06FoAH188=',
        'e2e.full@example.test',
        'E2E 정원 참가자',
        'ACTIVE'
    );
