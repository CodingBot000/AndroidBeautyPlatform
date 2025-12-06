(function() {
    var called = false;
    
    function notifyOnce() {
        if (called) return;
        called = true;
        
        // requestAnimationFrame 지원 여부 체크
        if (typeof requestAnimationFrame === 'function') {
            requestAnimationFrame(function() {
                requestAnimationFrame(function() {
                    Android.onLoadComplete();
                });
            });
        } else {
            // 구형 브라우저 fallback
            setTimeout(function() {
                Android.onLoadComplete();
            }, 100);
        }
    }
    
    // 이미 로드 완료된 경우
    if (document.readyState === 'complete') {
        notifyOnce();
        return;
    }
    
    // window.onload (가장 안전한 방법)
    window.addEventListener('load', notifyOnce);
    
    // 안전장치: load 이벤트 누락 대비
    document.addEventListener('readystatechange', function() {
        if (document.readyState === 'complete') {
            notifyOnce();
        }
    });
    
})();