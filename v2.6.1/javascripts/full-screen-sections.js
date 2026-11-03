document.addEventListener('DOMContentLoaded', function() {
  const content = document.querySelector('.md-content__inner');
  if (!content) return;

  // Get all major sections (h2 elements and their following content)
  const h2Elements = content.querySelectorAll('h2');

  h2Elements.forEach((h2, index) => {
    // Create a wrapper for each section
    const sectionWrapper = document.createElement('div');
    sectionWrapper.className = 'section-full-screen';
    sectionWrapper.id = `section-${index}`;

    // Insert the wrapper before the h2
    h2.parentNode.insertBefore(sectionWrapper, h2);

    // Move the h2 into the wrapper
    sectionWrapper.appendChild(h2);

    // Move all subsequent elements until the next h2 or end
    let nextElement = sectionWrapper.nextElementSibling;
    while (nextElement && nextElement.tagName !== 'H2') {
      const currentElement = nextElement;
      nextElement = nextElement.nextElementSibling;
      sectionWrapper.appendChild(currentElement);
    }
  });

  // Wrap the hero section (content before first h2) if it exists
  const firstH2 = content.querySelector('h2');
  if (firstH2) {
    const heroWrapper = document.createElement('div');
    heroWrapper.className = 'section-full-screen';
    heroWrapper.id = 'hero-section';

    // Move all elements before the first h2 into the hero wrapper
    const elementsBeforeH2 = [];
    let element = content.firstElementChild;
    while (element && element !== firstH2.parentElement) {
      if (element.tagName !== 'H2') {
        elementsBeforeH2.push(element);
      }
      element = element.nextElementSibling;
    }

    if (elementsBeforeH2.length > 0) {
      content.insertBefore(heroWrapper, content.firstElementChild);
      elementsBeforeH2.forEach(el => heroWrapper.appendChild(el));
    }
  }
});