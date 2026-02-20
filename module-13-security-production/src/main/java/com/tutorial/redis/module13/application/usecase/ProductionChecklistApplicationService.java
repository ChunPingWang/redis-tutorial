package com.tutorial.redis.module13.application.usecase;

import com.tutorial.redis.module13.domain.model.ProductionCheckItem;
import com.tutorial.redis.module13.domain.port.inbound.ProductionChecklistUseCase;
import com.tutorial.redis.module13.domain.service.ProductionChecklistService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Application service implementing the production checklist use case.
 *
 * <p>Delegates to the {@link ProductionChecklistService} domain service
 * which encapsulates the knowledge of what constitutes a production-ready
 * Redis deployment.</p>
 */
@Service
public class ProductionChecklistApplicationService implements ProductionChecklistUseCase {

    private static final Logger log = LoggerFactory.getLogger(ProductionChecklistApplicationService.class);

    private final ProductionChecklistService productionChecklistService;

    public ProductionChecklistApplicationService(ProductionChecklistService productionChecklistService) {
        this.productionChecklistService = productionChecklistService;
    }

    @Override
    public List<ProductionCheckItem> getChecklist() {
        log.info("Retrieving production readiness checklist");
        return productionChecklistService.getChecklist();
    }
}
