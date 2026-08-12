package com.sanjay.aisecurity.service;

import com.sanjay.aisecurity.dto.response.DashboardResponse;

/**
 * Service for computing dashboard analytics for the authenticated user.
 *
 * @author Sanjay
 * @version 1.0.0
 */
public interface DashboardService {

    /**
     * Returns aggregated analytics for the currently authenticated user.
     *
     * @return dashboard analytics response
     */
    DashboardResponse getMyDashboard();
}
