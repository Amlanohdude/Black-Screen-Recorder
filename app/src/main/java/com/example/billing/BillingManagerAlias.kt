package com.example.billing

/**
 * Type alias preserving backward-compatibility across tests and legacy callers.
 * Resolves to the active flavor's AppBillingManager implementation.
 */
typealias BillingManager = AppBillingManager
