// Simplified Skeleton Loading System for Janssen Documentation
class SkeletonLoader {
  constructor() {
    this.activeTimeouts = new Set();
    this.init();
  }

  init() {
    // Simple initialization - just observe elements that need loading animations
    this.setupContentObserver();
  }

  // Create simple skeleton templates
  createSkeletonHTML(type) {
    const templates = {
      featureCard: `
        <div class="skeleton-card skeleton">
          <div class="skeleton skeleton-title" style="height: 1.5rem; width: 60%; margin-bottom: 1rem;"></div>
          <div class="skeleton skeleton-paragraph" style="height: 1rem; width: 85%; margin-bottom: 0.5rem;"></div>
          <div class="skeleton skeleton-paragraph" style="height: 1rem; width: 70%; margin-bottom: 0.5rem;"></div>
          <div class="skeleton skeleton-paragraph" style="height: 1rem; width: 80%;"></div>
        </div>
      `,
      tabComponent: `
        <div class="skeleton-tab skeleton">
          <div class="skeleton skeleton-text large" style="width: 30%; height: 1.5rem; margin-bottom: 1rem;"></div>
          <div class="skeleton skeleton-code" style="height: 6rem;"></div>
        </div>
      `,
      default: `
        <div class="skeleton skeleton-text" style="height: 1rem; width: 80%; margin-bottom: 0.5rem;"></div>
        <div class="skeleton skeleton-text" style="height: 1rem; width: 70%; margin-bottom: 0.5rem;"></div>
        <div class="skeleton skeleton-text" style="height: 1rem; width: 85%;"></div>
      `
    };

    return templates[type] || templates.default;
  }

  // Simple loading simulation
  simulateLoading(element, duration = 1000) {
    if (!element) return;

    // Store original content
    const originalContent = element.innerHTML;
    const skeletonType = element.dataset.skeletonType || 'default';

    // Show skeleton immediately
    element.innerHTML = this.createSkeletonHTML(skeletonType);
    element.classList.add('content-loading');

    // Set timeout to restore content
    const timeoutId = setTimeout(() => {
      element.innerHTML = originalContent;
      element.classList.remove('content-loading');
      this.activeTimeouts.delete(timeoutId);
    }, duration);

    this.activeTimeouts.add(timeoutId);
  }

  // Simple content observer
  setupContentObserver() {
    const observer = new IntersectionObserver((entries) => {
      entries.forEach(entry => {
        if (entry.isIntersecting) {
          const element = entry.target;

          // Only simulate loading if element has the simulate-loading class
          if (element.classList.contains('simulate-loading')) {
            const duration = parseInt(element.dataset.loadingDuration) || 800;
            this.simulateLoading(element, duration);

            // Stop observing this element after first load
            observer.unobserve(element);
          }
        }
      });
    }, {
      threshold: 0.1,
      rootMargin: '50px'
    });

    // Observe all elements with simulate-loading class
    document.querySelectorAll('.simulate-loading').forEach(el => {
      observer.observe(el);
    });
  }

  // Cleanup method
  cleanup() {
    this.activeTimeouts.forEach(timeoutId => {
      clearTimeout(timeoutId);
    });
    this.activeTimeouts.clear();
  }
}

// Initialize skeleton loader when DOM is ready
document.addEventListener('DOMContentLoaded', () => {
  window.skeletonLoader = new SkeletonLoader();
  console.log('Simplified skeleton loader initialized');
});

// Cleanup on page unload
window.addEventListener('beforeunload', () => {
  if (window.skeletonLoader) {
    window.skeletonLoader.cleanup();
  }
});

// Export for module usage
if (typeof module !== 'undefined' && module.exports) {
  module.exports = SkeletonLoader;
}