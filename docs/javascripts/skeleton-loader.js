// Dynamic Skeleton Loading System for Janssen Documentation
class SkeletonLoader {
  constructor() {
    this.loadingStates = new Map();
    this.observers = new Map();
    this.init();
  }

  init() {
    // Initialize skeleton loaders for different content types
    this.setupIntersectionObserver();
    this.preloadSkeletons();
  }

  // Create skeleton templates for different content types
  createSkeletonTemplates() {
    return {
      featureCard: `
        <div class="skeleton-card skeleton">
          <div class="skeleton skeleton-title"></div>
          <div class="skeleton skeleton-paragraph"></div>
          <div class="skeleton skeleton-paragraph"></div>
          <div class="skeleton skeleton-paragraph"></div>
        </div>
      `,

      tabComponent: `
        <div class="skeleton-tab skeleton">
          <div class="skeleton skeleton-text large" style="width: 30%; margin-bottom: 1rem;"></div>
          <div class="skeleton skeleton-code"></div>
        </div>
      `,

      codeBlock: `
        <div class="skeleton-code skeleton">
          <div class="skeleton skeleton-text" style="width: 40%; margin-bottom: 0.5rem;"></div>
          <div class="skeleton skeleton-text" style="width: 60%; margin-bottom: 0.5rem;"></div>
          <div class="skeleton skeleton-text" style="width: 50%; margin-bottom: 0.5rem;"></div>
          <div class="skeleton skeleton-text" style="width: 70%;"></div>
        </div>
      `,

      heroSection: `
        <div class="skeleton-hero">
          <div class="skeleton skeleton-title" style="width: 80%; height: 3rem; margin-bottom: 1.5rem;"></div>
          <div class="skeleton skeleton-paragraph" style="width: 90%; height: 1.25rem; margin-bottom: 0.75rem;"></div>
          <div class="skeleton skeleton-paragraph" style="width: 85%; height: 1.25rem; margin-bottom: 2rem;"></div>
          <div style="display: flex; gap: 1rem;">
            <div class="skeleton skeleton-button"></div>
            <div class="skeleton skeleton-button"></div>
          </div>
        </div>
      `,

      quickStartSection: `
        <div class="skeleton-quickstart">
          <div class="skeleton skeleton-title" style="width: 40%; margin-bottom: 1.5rem;"></div>
          <div class="skeleton skeleton-tab" style="margin-bottom: 1rem;"></div>
          <div class="skeleton skeleton-code" style="height: 8rem;"></div>
        </div>
      `
    };
  }

  // Show skeleton loading for specific content sections
  showSkeletonFor(sectionSelector, skeletonType, duration = 1500) {
    const section = document.querySelector(sectionSelector);
    if (!section) return;

    const templates = this.createSkeletonTemplates();
    const skeletonHTML = templates[skeletonType];

    if (!skeletonHTML) return;

    // Store original content
    const originalContent = section.innerHTML;
    this.loadingStates.set(sectionSelector, originalContent);

    // Add loading class and inject skeleton
    section.classList.add('content-loading');
    section.innerHTML = skeletonHTML;

    // Restore content after duration
    setTimeout(() => {
      this.hideSkeletonFor(sectionSelector);
    }, duration);
  }

  // Hide skeleton and restore original content
  hideSkeletonFor(sectionSelector) {
    const section = document.querySelector(sectionSelector);
    const originalContent = this.loadingStates.get(sectionSelector);

    if (!section || !originalContent) return;

    section.classList.remove('content-loading');
    section.innerHTML = originalContent;
    this.loadingStates.delete(sectionSelector);

    // Re-trigger slide-in animations
    this.triggerSlideInAnimations(section);
  }

  // Setup intersection observer for lazy loading animations
  setupIntersectionObserver() {
    const observerOptions = {
      threshold: 0.1,
      rootMargin: '50px'
    };

    this.contentObserver = new IntersectionObserver((entries) => {
      entries.forEach(entry => {
        if (entry.isIntersecting) {
          const element = entry.target;

          // Add loading simulation for dynamic content
          if (element.classList.contains('simulate-loading')) {
            this.simulateContentLoading(element);
          }

          // Trigger slide-in animations
          this.triggerSlideInAnimations(element);
        }
      });
    }, observerOptions);
  }

  // Simulate content loading with skeletons
  simulateContentLoading(element) {
    const sectionType = element.dataset.skeletonType;
    const loadingDuration = parseInt(element.dataset.loadingDuration) || 1200;

    if (sectionType) {
      const selector = `#${element.id}` || `.${element.className.split(' ')[0]}`;
      this.showSkeletonFor(selector, sectionType, loadingDuration);
    }
  }

  // Re-trigger slide-in animations after content loads
  triggerSlideInAnimations(container) {
    const animatedElements = container.querySelectorAll('[class*="slideIn"], [class*="fadeIn"]');

    animatedElements.forEach(el => {
      el.style.animation = 'none';
      el.offsetHeight; // Trigger reflow
      el.style.animation = null;
    });
  }

  // Preload skeletons for immediate use
  preloadSkeletons() {
    // Observe sections that should have loading animations
    const sectionsToObserve = [
      '.why-janssen-grid',
      '.tabbed-set',
      '.hero-section',
      '.quick-start-section'
    ];

    sectionsToObserve.forEach(selector => {
      const elements = document.querySelectorAll(selector);
      elements.forEach(el => {
        this.contentObserver.observe(el);
      });
    });
  }

  // Public API methods
  loadFeatureCards(containerSelector, count = 6, duration = 1500) {
    const container = document.querySelector(containerSelector);
    if (!container) return;

    const skeletonCards = Array(count).fill(null).map(() =>
      this.createSkeletonTemplates().featureCard
    ).join('');

    container.innerHTML = skeletonCards;
    container.classList.add('content-loading');

    setTimeout(() => {
      container.classList.remove('content-loading');
      // Restore original content would happen here
    }, duration);
  }

  loadTabs(containerSelector, duration = 1200) {
    this.showSkeletonFor(containerSelector, 'tabComponent', duration);
  }

  loadCodeBlocks(containerSelector, duration = 1000) {
    this.showSkeletonFor(containerSelector, 'codeBlock', duration);
  }

  loadHeroSection(duration = 800) {
    this.showSkeletonFor('.hero-section', 'heroSection', duration);
  }

  simulateLoading(element, duration = 1000) {
    if (!element) return;

    const skeletonType = element.dataset.skeletonType || 'featureCard';

    // Create skeleton elements based on type
    const templates = this.createSkeletonTemplates();
    let skeletonHTML = '';

    switch(skeletonType) {
      case 'featureCard':
        skeletonHTML = Array(6).fill(templates.featureCard).join('');
        break;
      case 'tabComponent':
        skeletonHTML = templates.tabComponent;
        break;
      case 'quickStartSection':
        skeletonHTML = templates.codeBlock;
        break;
      default:
        skeletonHTML = templates.textBlock;
    }

    // Store original content
    const originalContent = element.innerHTML;

    // Show skeleton
    element.innerHTML = skeletonHTML;
    element.classList.add('content-loading');

    // Restore content after duration
    setTimeout(() => {
      element.innerHTML = originalContent;
      element.classList.remove('content-loading');
    }, duration);
  }
}

// Initialize skeleton loader when DOM is ready
document.addEventListener('DOMContentLoaded', () => {
  window.skeletonLoader = new SkeletonLoader();

  // Auto-trigger loading for elements with simulate-loading class
  const autoLoadElements = document.querySelectorAll('.simulate-loading');
  autoLoadElements.forEach((element, index) => {
    const duration = parseInt(element.dataset.loadingDuration) || 1000;
    const skeletonType = element.dataset.skeletonType || 'default';

    // Add a small staggered delay for better visual effect
    setTimeout(() => {
      window.skeletonLoader.simulateLoading(element, duration);
    }, index * 200);
  });

  console.log('Skeleton loader initialized with auto-loading');
});

// Export for module usage
if (typeof module !== 'undefined' && module.exports) {
  module.exports = SkeletonLoader;
}