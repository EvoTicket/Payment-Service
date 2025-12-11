package com.capstone.paymentservice.security;

public record TokenMetaData(Long userId, boolean isOrganization, Long organizationId) {
}
