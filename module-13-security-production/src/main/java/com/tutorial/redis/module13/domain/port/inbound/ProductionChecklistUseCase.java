package com.tutorial.redis.module13.domain.port.inbound;

import com.tutorial.redis.module13.domain.model.ProductionCheckItem;

import java.util.List;

/**
 * Inbound port for the production-readiness checklist use case.
 *
 * <p>Provides a comprehensive list of best-practice checks that should
 * be verified before deploying Redis to a production environment.</p>
 */
public interface ProductionChecklistUseCase {

    /**
     * Returns the full production-readiness checklist.
     *
     * @return list of check items grouped by category
     */
    List<ProductionCheckItem> getChecklist();
}
