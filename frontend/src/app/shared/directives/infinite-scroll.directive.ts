import {
  Directive,
  ElementRef,
  OnInit,
  OnDestroy,
  output,
  input,
} from '@angular/core';

@Directive({
  selector: '[appInfiniteScroll]',
})
export class InfiniteScrollDirective implements OnInit, OnDestroy {
  enabled = input(true);

  scrolled = output<void>();

  private observer: IntersectionObserver | null = null;

  constructor(private el: ElementRef<HTMLElement>) {}

  ngOnInit(): void {
    this.observer = new IntersectionObserver(
      (entries) => {
        if (entries[0].isIntersecting && this.enabled()) {
          this.scrolled.emit();
        }
      },
      {
        root: null,
        rootMargin: '200px',
        threshold: 0,
      }
    );
    this.observer.observe(this.el.nativeElement);
  }

  ngOnDestroy(): void {
    this.observer?.disconnect();
    this.observer = null;
  }
}
