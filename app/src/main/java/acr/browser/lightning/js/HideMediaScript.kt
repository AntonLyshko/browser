package acr.browser.lightning.js

object HideMediaScript {

    val JS: String = """
        (function() {
            if (window.__hmInstalled) {
                if (window.__hmEnsure) window.__hmEnsure();
                return;
            }
            window.__hmInstalled = true;

            var STYLE_ID = '__hm_hide_media_style__';
            var CSS = [
                'img, video, picture, source, canvas, svg, area, input[type="image"],',
                'object:not([type^="text/"]):not([type="application/pdf"]),',
                'embed:not([type^="text/"]):not([type="application/pdf"]) {',
                '  opacity: 0 !important;',
                '}',
                '*, *::before, *::after {',
                '  background-image: none !important;',
                '  border-image-source: none !important;',
                '  list-style-image: none !important;',
                '}',
                '[data-hm-content-url="1"] { opacity: 0 !important; }',
                '[data-hm-mask-url="1"] { opacity: 0 !important; }',
                '[data-hm-before-url="1"]::before { content: "" !important; opacity: 0 !important; }',
                '[data-hm-after-url="1"]::after { content: "" !important; opacity: 0 !important; }'
            ].join('\n');

            var MEDIA_TAGS = {
                IMG: 1,
                VIDEO: 1,
                PICTURE: 1,
                SOURCE: 1,
                CANVAS: 1,
                SVG: 1,
                AREA: 1
            };

            var observedRoots = typeof WeakSet !== 'undefined' ? new WeakSet() : null;
            var computedDone = typeof WeakSet !== 'undefined' ? new WeakSet() : null;
            var pending = [];
            var rafScheduled = false;

            function isMediaElement(el) {
                if (!el || el.nodeType !== 1) return false;
                var tag = el.tagName ? el.tagName.toUpperCase() : '';
                if (MEDIA_TAGS[tag]) return true;
                if (tag === 'INPUT' && el.type && el.type.toLowerCase() === 'image') return true;
                if (tag === 'OBJECT' || tag === 'EMBED') {
                    var type = (el.getAttribute('type') || '').toLowerCase();
                    if (!type || type.indexOf('image/') === 0 || type.indexOf('video/') === 0) {
                        return true;
                    }
                }
                return false;
            }

            function injectStyle(root) {
                if (!root) return false;
                var existing = root.querySelector ? root.querySelector('#' + STYLE_ID) : null;
                if (existing) return true;

                var target = root === document ? (document.head || document.documentElement) : root;
                if (!target) return false;

                var style = document.createElement('style');
                style.id = STYLE_ID;
                style.textContent = CSS;
                target.appendChild(style);
                return true;
            }

            function checkComputed(el) {
                if (!el || el.nodeType !== 1 || !el.isConnected) return;
                if (computedDone) {
                    if (computedDone.has(el)) return;
                    computedDone.add(el);
                }
                try {
                    var cs = getComputedStyle(el);
                    if (cs) {
                        if (cs.content && cs.content.indexOf('url(') !== -1) {
                            el.setAttribute('data-hm-content-url', '1');
                        }
                        var mask = cs.maskImage || cs.webkitMaskImage || '';
                        if (mask.indexOf('url(') !== -1) {
                            el.setAttribute('data-hm-mask-url', '1');
                        }
                    }
                    var before = getComputedStyle(el, '::before');
                    if (before && before.content && before.content.indexOf('url(') !== -1) {
                        el.setAttribute('data-hm-before-url', '1');
                    }
                    var after = getComputedStyle(el, '::after');
                    if (after && after.content && after.content.indexOf('url(') !== -1) {
                        el.setAttribute('data-hm-after-url', '1');
                    }
                } catch (e) {}
            }

            function flushPending() {
                rafScheduled = false;
                var batch = pending;
                pending = [];
                for (var i = 0; i < batch.length; i++) {
                    checkComputed(batch[i]);
                }
            }

            function scheduleComputed(el) {
                pending.push(el);
                if (!rafScheduled) {
                    rafScheduled = true;
                    if (typeof requestAnimationFrame === 'function') {
                        requestAnimationFrame(flushPending);
                    } else {
                        setTimeout(flushPending, 16);
                    }
                }
            }

            function processElement(el) {
                if (!el || el.nodeType !== 1) return;

                if (isMediaElement(el) && el.style && el.style.opacity !== '0') {
                    el.style.setProperty('opacity', '0', 'important');
                }
                if (el.style) {
                    if (el.style.backgroundImage && el.style.backgroundImage !== 'none') {
                        el.style.setProperty('background-image', 'none', 'important');
                    }
                    if (el.style.maskImage && el.style.maskImage.indexOf('url(') !== -1) {
                        el.style.setProperty('opacity', '0', 'important');
                    }
                    if (el.style.webkitMaskImage && el.style.webkitMaskImage.indexOf('url(') !== -1) {
                        el.style.setProperty('opacity', '0', 'important');
                    }
                }
                if (el.tagName && el.tagName.toUpperCase() === 'IFRAME') {
                    try {
                        if (el.contentDocument) setupRoot(el.contentDocument);
                    } catch (e) {}
                    if (!el.__hmFrameHooked) {
                        el.__hmFrameHooked = true;
                        el.addEventListener('load', function() {
                            try {
                                if (el.contentDocument) setupRoot(el.contentDocument);
                            } catch (e) {}
                        });
                    }
                }
                if (el.shadowRoot) {
                    setupRoot(el.shadowRoot);
                }
                scheduleComputed(el);
            }

            function processTree(node) {
                if (!node || node.nodeType !== 1) return;
                processElement(node);
                if (node.querySelectorAll) {
                    var children = node.querySelectorAll('*');
                    for (var i = 0; i < children.length; i++) {
                        processElement(children[i]);
                    }
                }
            }

            function setupRoot(root) {
                if (!root) return;
                injectStyle(root);

                if (observedRoots) {
                    if (observedRoots.has(root)) return;
                    observedRoots.add(root);
                } else if (root.__hmObserved) {
                    return;
                } else {
                    root.__hmObserved = true;
                }

                if (root.querySelectorAll) {
                    var all = root.querySelectorAll('*');
                    for (var i = 0; i < all.length; i++) {
                        processElement(all[i]);
                    }
                }

                if (typeof MutationObserver !== 'undefined') {
                    var observer = new MutationObserver(function(mutations) {
                        injectStyle(root);
                        for (var i = 0; i < mutations.length; i++) {
                            var m = mutations[i];
                            if (m.type === 'childList') {
                                for (var j = 0; j < m.addedNodes.length; j++) {
                                    processTree(m.addedNodes[j]);
                                }
                            } else if (m.type === 'attributes') {
                                if (computedDone) computedDone.delete(m.target);
                                processElement(m.target);
                            }
                        }
                    });
                    observer.observe(root, {
                        childList: true,
                        subtree: true,
                        attributes: true,
                        attributeFilter: ['style', 'class']
                    });
                }
            }

            var origAttachShadow = Element.prototype.attachShadow;
            if (origAttachShadow && !origAttachShadow.__hmPatched) {
                var patchedAttachShadow = function(init) {
                    var shadowRoot = origAttachShadow.call(this, init);
                    setupRoot(shadowRoot);
                    return shadowRoot;
                };
                patchedAttachShadow.__hmPatched = true;
                Element.prototype.attachShadow = patchedAttachShadow;
            }

            window.__hmEnsure = function() {
                setupRoot(document);
            };

            setupRoot(document);
            if (document.readyState === 'loading') {
                document.addEventListener('DOMContentLoaded', function() {
                    setupRoot(document);
                });
            }
        })();
    """.trimIndent()
}